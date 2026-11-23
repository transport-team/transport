package com.transport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Member;
import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.client.TossPaymentClient;
import com.transport.dto.DailyStatDTO;
import com.transport.dto.PaidStatDTO;
import com.transport.dto.PaymentApproveRequestDTO;
import com.transport.dto.PaymentConfirmResponseDTO;
import com.transport.dto.VehicleTypeStatDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.PaymentRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Service
@RequiredArgsConstructor
@Log4j2
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final TossPaymentClient tossPaymentClient;
    private final StatusHistoryService statusHistoryService;
    private final DispatchApplicationService dispatchApplicationService;  // 팀원 2 (취소 때 대기 지원 마감)
    private final DispatchService dispatchService;                        // 팀원 2 (관리자 취소: 배정 기사 · 배차 해제)
    private final NotificationService notificationService;                // 팀원 4 (관리자 취소: 고객 · 기사 알림)
    private final MemberService memberService;                            // 팀원 1 (취소한 관리자 · 알림 받을 기사)

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public PaymentConfirmResponseDTO verifyAndApprovePayment(PaymentApproveRequestDTO requestDTO, Long memberId) {
        Payment payment = paymentRepository.findByOrderIdForUpdate(requestDTO.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Shipment shipment = payment.getShipment();

        if (!shipment.getCustomer().getId().equals(memberId)
                || shipment.getCustomer().getRole() != MemberRole.CUSTOMER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        if (payment.getStatus() != PaymentStatus.READY
                || shipment.getStatus() != ShipmentStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }

        if (payment.getAmount() != requestDTO.getAmount()
                || shipment.getFare() != requestDTO.getAmount()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        tossPaymentClient.confirm(requestDTO.getPaymentKey(), requestDTO.getOrderId(), requestDTO.getAmount());

        try {
            ShipmentStatus fromStatus = shipment.getStatus();
            payment.updatePaymentKey(requestDTO.getPaymentKey());
            payment.updateStatus(PaymentStatus.PAID);
            payment.updatePaidAt(LocalDateTime.now());
            shipment.changeStatus(ShipmentStatus.REQUESTED); // 전이 규칙 검사 (PENDING_PAYMENT → REQUESTED)
            statusHistoryService.record(shipment, fromStatus, ShipmentStatus.REQUESTED, null, "토스 결제 승인");
            entityManager.flush();
            return PaymentConfirmResponseDTO.from(payment);
        } catch (RuntimeException databaseException) {
            try {
                tossPaymentClient.cancelPayment(requestDTO.getPaymentKey(), "결제 승인 후 배송 저장 실패");
            } catch (RuntimeException cancelException) {
                log.error("결제 승인 후 저장 실패 및 토스 결제 취소 실패: orderId={}",
                        requestDTO.getOrderId(), cancelException);
            }
            throw databaseException;
        }
    }

    // ===== 관리자 대시보드 집계 (팀원 1 대시보드가 쓰는 읽기 전용 메서드) =====

    @Override
    public PaidStatDTO getPaidStat(LocalDateTime from, LocalDateTime to) {
        PaymentRepository.PaidSum sum = paymentRepository.sumByPaidAt(PaymentStatus.PAID, from, to);
        return PaidStatDTO.builder()
                .count(sum.getPaidCount())
                .revenue(sum.getRevenue())
                .build();
    }

    @Override
    public List<DailyStatDTO> getDailyPaidStats(LocalDate fromDate, LocalDate toDate) {
        // toDate 하루 전체를 넣으려고 다음 날 0시 미만으로 찾는다
        return paymentRepository.sumGroupByPaidDate(PaymentStatus.PAID, fromDate.atStartOfDay(), toDate.plusDays(1).atStartOfDay())
                .stream()
                .map(row -> DailyStatDTO.builder()
                        .date(row.getPaidDate())
                        .count(row.getPaidCount())
                        .revenue(row.getRevenue())
                        .build())
                .toList();
    }

    @Override
    public List<VehicleTypeStatDTO> getPaidStatsByVehicleType(LocalDateTime from, LocalDateTime to) {
        return paymentRepository.sumGroupByVehicleType(PaymentStatus.PAID, from, to)
                .stream()
                .map(row -> VehicleTypeStatDTO.builder()
                        .vehicleTypeId(row.getVehicleTypeId())
                        .vehicleTypeName(row.getVehicleTypeName())
                        .count(row.getPaidCount())
                        .revenue(row.getRevenue())
                        .build())
                .toList();
    }

    // 고객 취소 (CUS-06): 배차 전(결제 대기 · 접수)에만. 배차가 확정된 뒤 취소는 관리자 몫이다
    // (status.md — 배차 해제 · 기사 알림이 같이 필요, #86)
    // 순서: DB(결제 · 배송 · 이력 · 대기 지원 마감)를 먼저 저장하고 토스 환불은 마지막 — 환불이 실패하면 예외로 전부 롤백된다
    @Override
    @Transactional
    public PaymentConfirmResponseDTO cancelPayment(Long shipmentId, String cancelReason, Long memberId) {
        // 동시에 두 번 눌러도 한 번만 취소 · 환불되게 결제 · 배송을 잠그고 읽는다 (승인과 같은 방식)
        Payment payment = paymentRepository.findByShipmentIdForUpdate(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Shipment shipment = payment.getShipment();

        // 1. 본인 및 권한 확인
        if (!shipment.getCustomer().getId().equals(memberId)
                || shipment.getCustomer().getRole() != MemberRole.CUSTOMER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 2. 고객은 결제 대기 · 접수일 때만 취소할 수 있다
        ShipmentStatus fromStatus = shipment.getStatus();
        if (fromStatus != ShipmentStatus.PENDING_PAYMENT && fromStatus != ShipmentStatus.REQUESTED) {
            throw new CustomException(ErrorCode.CANNOT_CANCEL, cannotCancelMessage(fromStatus));
        }

        // 3. DB 먼저: 결제 · 배송 취소, 이력, 대기 중인 기사 지원 마감 → flush로 저장 오류를 환불 전에 확인
        boolean paid = payment.getStatus() == PaymentStatus.PAID;
        payment.cancel(cancelReason);
        shipment.cancel(cancelReason);
        statusHistoryService.record(
                shipment,
                fromStatus,
                ShipmentStatus.CANCELED,
                shipment.getCustomer(),
                "고객 취소: " + cancelReason
        );
        dispatchApplicationService.closePendingApplications(shipment);  // 팀원 2 약속 메서드
        entityManager.flush();

        // 4. 결제 완료 건만 토스 환불 (개발용 결제 건너뛰기 건은 토스에 없으니 뺀다)
        String paymentKey = payment.getPaymentKey();
        if (paid && paymentKey != null && !paymentKey.startsWith("LOCAL_SKIP_")) {
            tossPaymentClient.cancelPayment(paymentKey, cancelReason);
        }

        return PaymentConfirmResponseDTO.from(payment);
    }

    // 관리자 취소 (ADM-02): 결제 대기 · 접수 · 배차 완료일 때. 배차된 건은 배차 해제 · 기사 알림까지 한다
    // (status.md '배차완료 → 취소', 11-19 결정 34). 상차 뒤에는 운송이 시작돼 취소하지 않는다
    // 순서는 고객 취소와 같다: DB(결제 · 배송 · 이력 · 대기 지원 마감 · 배차 해제 · 알림)를 먼저 저장하고 토스 환불은 마지막
    // — 환불이 실패하면 예외로 전부 롤백되고, 알림도 커밋 뒤에만 나가므로 보내지지 않는다
    @Override
    @Transactional
    public PaymentConfirmResponseDTO cancelByAdmin(Long shipmentId, String cancelReason, Long adminMemberId) {
        Payment payment = paymentRepository.findByShipmentIdForUpdate(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Shipment shipment = payment.getShipment();

        // 1. 결제 대기 · 접수 · 배차 완료만 취소할 수 있다
        ShipmentStatus fromStatus = shipment.getStatus();
        if (fromStatus != ShipmentStatus.PENDING_PAYMENT && fromStatus != ShipmentStatus.REQUESTED
                && fromStatus != ShipmentStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.CANNOT_CANCEL, adminCannotCancelMessage(fromStatus));
        }

        // 2. 배차된 건이면 알림 받을 배정 기사(회원 ID)를 해제 전에 찾아 둔다
        boolean assigned = fromStatus == ShipmentStatus.ASSIGNED;
        Long driverMemberId = assigned ? dispatchService.getActiveDriver(shipmentId) : null;
        Member admin = memberService.findById(adminMemberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "관리자 정보를 찾을 수 없습니다."));

        // 3. DB 먼저: 결제 · 배송 취소, 이력, 대기 지원 마감, 배차 해제 → flush로 저장 오류를 환불 전에 확인
        boolean paid = payment.getStatus() == PaymentStatus.PAID;
        payment.cancel(cancelReason);
        shipment.cancel(cancelReason);
        statusHistoryService.record(shipment, fromStatus, ShipmentStatus.CANCELED, admin, "관리자 취소: " + cancelReason);
        dispatchApplicationService.closePendingApplications(shipment);  // 팀원 2 약속 메서드
        if (assigned) {
            dispatchService.releaseDispatch(shipmentId);                  // 팀원 2 약속 메서드 (#123)
        }
        entityManager.flush();

        // 4. 알림: 고객, 배차된 건은 배정 기사도
        notificationService.send(shipment.getCustomer(), NotificationType.STATUS_CHANGED,
                "배송 " + shipmentId + "번이 관리자에 의해 취소되었습니다. 사유: " + cancelReason
                        + (paid ? " (결제 금액은 환불됩니다)" : ""),
                shipment);
        if (driverMemberId != null) {
            memberService.findById(driverMemberId).ifPresent(driver -> notificationService.send(driver,
                    NotificationType.STATUS_CHANGED,
                    "배송 " + shipmentId + "번이 관리자에 의해 취소되어 배차가 해제되었습니다. 사유: " + cancelReason,
                    shipment));
        }

        // 5. 결제 완료 건만 토스 환불 (개발용 결제 건너뛰기 건은 토스에 없으니 뺀다)
        String paymentKey = payment.getPaymentKey();
        if (paid && paymentKey != null && !paymentKey.startsWith("LOCAL_SKIP_")) {
            tossPaymentClient.cancelPayment(paymentKey, cancelReason);
        }

        return PaymentConfirmResponseDTO.from(payment);
    }

    @Override
    @Transactional
    public PaymentConfirmResponseDTO retryPayment(Long shipmentId, Long memberId) {
        Payment payment = paymentRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        Shipment shipment = payment.getShipment();

        // 1. 본인 및 고객 권한 확인
        if (!shipment.getCustomer().getId().equals(memberId)
                || shipment.getCustomer().getRole() != MemberRole.CUSTOMER) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 2. 이미 결제 완료되었거나 취소된 건은 재시도 불가
        if (payment.getStatus() == PaymentStatus.PAID) {
            throw new CustomException(ErrorCode.PAYMENT_ALREADY_COMPLETED);
        }
        if (payment.getStatus() != PaymentStatus.READY
                || shipment.getStatus() != ShipmentStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }

        // 3. 30분 만료 여부 체크 (스케줄러 실행 전이라도 30분이 지났으면 재시도 차단)
        if (shipment.getCreatedAt().isBefore(LocalDateTime.now().minusMinutes(30))) {
            throw new CustomException(ErrorCode.PAYMENT_NOT_ALLOWED);
        }

        // 4. 새 orderId 발급 및 갱신
        String newOrderId = "ORD-" + java.util.UUID.randomUUID();
        payment.renewOrderId(newOrderId);

        return PaymentConfirmResponseDTO.from(payment);
    }

    // 고객이 취소할 수 없는 상태별 안내
    private static String cannotCancelMessage(ShipmentStatus status) {
        return switch (status) {
            case ASSIGNED -> "배차가 확정된 배송은 관리자에게 취소를 요청해 주세요.";
            case CANCELED -> "이미 취소된 배송입니다.";
            default -> "운송이 시작된 배송은 취소할 수 없습니다.";
        };
    }

    // 관리자가 취소할 수 없는 상태별 안내 (상차 뒤 · 이미 취소)
    private static String adminCannotCancelMessage(ShipmentStatus status) {
        return status == ShipmentStatus.CANCELED
                ? "이미 취소된 배송입니다."
                : "상차가 끝나 운송이 시작된 배송은 취소할 수 없습니다.";
    }
}