package com.transport.repository;

import com.transport.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 토스페이먼츠 결제 승인 시 고유 주문번호로 결제 내역을 찾기 위한 메서드
    Optional<Payment> findByOrderId(String orderId);
}