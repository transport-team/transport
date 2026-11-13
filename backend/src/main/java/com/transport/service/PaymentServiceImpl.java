package com.transport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.MemberRole;
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
}