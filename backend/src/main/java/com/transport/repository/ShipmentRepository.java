package com.transport.repository;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    // 향후 회원별 배송 내역 조회 기능 등을 위해 추가할 메서드 예시
    // List<Shipment> findAllByCustomerId(Long customerId);

	// 기사: 배차 대기 중인 운송 요청 목록 조회 (상차 예정일 빠른 순)
	List<Shipment> findByStatusOrderByPickupAtAsc(ShipmentStatus status);
}