package com.transport.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.PaymentApproveRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public void verifyAndApprovePayment(PaymentApproveRequestDTO requestDTO) {
        // 1. 주문번호로 결제 내역 조회 (PaymentRepository에 findByOrderId 메서드 필요)
        Payment payment = paymentRepository.findByOrderId(requestDTO.getOrderId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 2. 금액 검증 (서버 산출 금액 vs 프론트엔드 요청 금액 일치 여부)
        if (payment.getAmount() != requestDTO.getAmount()) {
            throw new CustomException(ErrorCode.PAYMENT_AMOUNT_MISMATCH);
        }

        // 3. 결제 상태 업데이트 
        // (실제 토스 API를 서버에서 한 번 더 호출해 승인받는 로직이 들어갈 수 있습니다)
        payment.updatePaymentKey(requestDTO.getPaymentKey());
        payment.updateStatus(PaymentStatus.PAID); // PaymentStatus.java에 PAID(결제완료) 있는지 확인
        payment.updatePaidAt(LocalDateTime.now());

        // 4. 연관된 배송 상태 업데이트 (결제대기 -> 접수완료/배차대기)
        Shipment shipment = payment.getShipment();
        shipment.updateStatus(ShipmentStatus.REQUESTED); 
    }
}