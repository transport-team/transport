package com.transport.repository;

import com.transport.domain.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    // 향후 회원별 배송 내역 조회 기능 등을 위해 추가할 메서드 예시
    // List<Shipment> findAllByCustomerId(Long customerId);
}