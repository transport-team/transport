package com.transport.controller;

import java.time.LocalDateTime;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.PaymentConfirmResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.PaymentRepository;
import com.transport.repository.ShipmentRepository;
import com.transport.security.LoginMember;
import com.transport.service.StatusHistoryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Profile("local")
@Tag(name = "Local Test API", description = "로컬 개발 및 시연 전용 결제 건너뛰기 API")
@RestController
@RequestMapping("/api/local/payments")
@RequiredArgsConstructor
public class LocalPaymentTestController {

    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final StatusHistoryService statusHistoryService;

    @Operation(summary = "[Local 전용] 결제 건너뛰기 (즉시 결제 완료)", 
               description = "토스페이먼츠 승인 절차 없이 해당 배송 건을 즉시 결제 완료(PAID) 및 배차 대기(REQUESTED) 상태로 전환합니다.")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/skip/{shipmentId}")
    @Transactional
    public ResponseEntity<PaymentConfirmResponseDTO> skipPayment(
            @PathVariable Long shipmentId,
            @AuthenticationPrincipal LoginMember loginMember) {

        // 1. 배송 조회 및 본인 확인
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

        if (!shipment.getCustomer().getId().equals(loginMember.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인의 배송 건만 처리할 수 있습니다.");
        }

        if (shipment.getStatus() != ShipmentStatus.PENDING_PAYMENT) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "결제 대기 상태인 배송만 건너뛸 수 있습니다.");
        }

        // 2. 결제 엔티티 조회
        Payment payment = paymentRepository.findByShipmentId(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "결제 정보를 찾을 수 없습니다."));

        // 3. Payment & Shipment 상태 업데이트 (Payment.java 기존 메서드 활용)
        String dummyPaymentKey = "LOCAL_SKIP_" + payment.getOrderId();
        payment.updatePaymentKey(dummyPaymentKey);
        payment.updateStatus(PaymentStatus.PAID);
        payment.updatePaidAt(LocalDateTime.now());

        shipment.changeStatus(ShipmentStatus.REQUESTED);

        // 4. 타임라인 이력 기록 (인자 5개 시그니처 맞춤)
        statusHistoryService.record(
                shipment,
                ShipmentStatus.PENDING_PAYMENT,
                ShipmentStatus.REQUESTED,
                shipment.getCustomer(),
                "결제 완료 (로컬 테스트 스킵)"
        );

        return ResponseEntity.ok(PaymentConfirmResponseDTO.from(payment));
    }
}