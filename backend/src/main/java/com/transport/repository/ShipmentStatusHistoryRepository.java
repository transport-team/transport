package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.ShipmentStatusHistory;

// 배송 상태 변경 이력 (팀원 4)
public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, Long> {

	// 타임라인 조회용: 배송 ID로 정렬된 이력 목록
	List<ShipmentStatusHistory> findByShipment_IdOrderByChangedAtAsc(Long shipmentId);
}