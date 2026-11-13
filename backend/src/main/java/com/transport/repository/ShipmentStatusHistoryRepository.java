package com.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;

// 배송 상태 변경 이력 (팀원 4)
public interface ShipmentStatusHistoryRepository extends JpaRepository<ShipmentStatusHistory, Long> {

	// 타임라인 조회용: 배송 ID로 정렬된 이력 목록
	@EntityGraph(attributePaths = "changedBy")
	List<ShipmentStatusHistory> findByShipment_IdOrderByChangedAtAsc(Long shipmentId);

	// 배송의 특정 상태 변경 이력 중 가장 최근 기록 조회
	Optional<ShipmentStatusHistory> findFirstByShipment_IdAndToStatusOrderByChangedAtDesc(
		Long shipmentId, ShipmentStatus toStatus);
}