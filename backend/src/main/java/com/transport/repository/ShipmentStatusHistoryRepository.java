package com.transport.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
  
	// ===== 관리자 대시보드 집계 (팀원 1 작성 · 팀원 4 리뷰, 기간은 changedAt 기준 [from, to) — from 이상, to 미만) =====

	// 기간 안에 그 상태로 바뀐 건수
	@Query("select count(h) from ShipmentStatusHistory h "
			+ "where h.toStatus = :status and h.changedAt >= :from and h.changedAt < :to")
	long countByToStatusBetween(@Param("status") ShipmentStatus status,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// 기간 안에 그 상태로 바꾼 사람이 기사인 건만 기사별로 센다 (건수 많은 순 → 기사 id 순, 몇 명까지는 pageable)
	@Query("select d.id as driverId, m.name as driverName, count(h) as changedCount "
			+ "from ShipmentStatusHistory h join h.changedBy m join Driver d on d.member = m "
			+ "where h.toStatus = :status and h.changedAt >= :from and h.changedAt < :to "
			+ "group by d.id, m.name order by count(h) desc, d.id")
	List<DriverChangedCount> countGroupByDriver(@Param("status") ShipmentStatus status,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

	// 기사별 건수 결과
	interface DriverChangedCount {
		Long getDriverId();

		String getDriverName();

		Long getChangedCount();
	}
}
