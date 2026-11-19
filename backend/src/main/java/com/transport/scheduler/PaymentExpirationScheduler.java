package com.transport.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.repository.PaymentRepository;
import com.transport.service.StatusHistoryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Component
@RequiredArgsConstructor
@Log4j2
public class PaymentExpirationScheduler {

    private static final String AUTO_CANCEL_REASON = "결제 유효시간(30분) 초과로 자동 취소";

    private final PaymentRepository paymentRepository;
    private final StatusHistoryService statusHistoryService;

    // 매 1분마다 실행 (이전 실행 종료 후 60초 대기)
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void cancelExpiredPendingPayments() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(30);

        List<Payment> expiredPayments = paymentRepository.findExpiredPendingPayments(
                PaymentStatus.READY,
                ShipmentStatus.PENDING_PAYMENT,
                cutoff
        );

        if (expiredPayments.isEmpty()) {
            return;
        }

        for (Payment payment : expiredPayments) {
            Shipment shipment = payment.getShipment();
            payment.cancel(AUTO_CANCEL_REASON);
            shipment.cancel(AUTO_CANCEL_REASON);
            statusHistoryService.record(
                    shipment,
                    ShipmentStatus.PENDING_PAYMENT,
                    ShipmentStatus.CANCELED,
                    null,
                    AUTO_CANCEL_REASON
            );
        }

        log.info("30분 미결제 배송 자동 취소 완료: {}건", expiredPayments.size());
    }
}