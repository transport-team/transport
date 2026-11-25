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

	// ===== 관리자 대시보드 집계 (팀원 1 작성 · 팀원 4 리뷰, 기간은 changedAt 기준 [from, to) — from 이상,
	// to 미만) =====

	// 기간 안에 그 상태로 바뀐 건수
	@Query("select count(h) from ShipmentStatusHistory h "
			+ "where h.toStatus = :status and h.changedAt >= :from and h.changedAt < :to")
	long countByToStatusBetween(@Param("status") ShipmentStatus status,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

	// 기간 안에 그 상태로 바뀐 이력을 배정 기사(활성 배차의 기사, 활성 배차가 없으면 변경자)별로 센다 (건수 많은 순 → 기사 id 순, 몇 명까지는 pageable)
	@Query("select d.id as driverId, d.member.name as driverName, count(h) as changedCount "
            + "from ShipmentStatusHistory h join Driver d on "
            + "(exists (select 1 from Dispatch a where a.shipment = h.shipment and a.active = true and a.driver = d) "
            + "or (not exists (select 1 from Dispatch a where a.shipment = h.shipment and a.active = true) "
            + "and d.member = h.changedBy)) "
            + "where h.toStatus = :status and h.changedAt >= :from and h.changedAt < :to "
            + "group by d.id, d.member.name order by count(h) desc, d.id")
	List<DriverChangedCount> countGroupByDriver(@Param("status") ShipmentStatus status,
			@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

	// 기사별 건수 결과
	interface DriverChangedCount {
		Long getDriverId();

		String getDriverName();

		Long getChangedCount();
	}

	// 기사별 월간 배송 완료 건수 및 운임 합계
	@Query("select count(h) as completedCount, "
			+ "coalesce(sum(h.shipment.fare), 0) as totalFare "
			+ "from ShipmentStatusHistory h "
            + "where h.toStatus = :status "
            + "and h.shipment.status = :status "
            + "and (exists (select 1 from Dispatch d where d.shipment = h.shipment "
            + "and d.active = true and d.driver.member.id = :memberId) "
            + "or (h.changedBy.id = :memberId and not exists "
            + "(select 1 from Dispatch d where d.shipment = h.shipment and d.active = true))) "
			+ "and h.changedAt >= :from and h.changedAt < :to")
	DriverMonthlyTotal findDriverMonthlyTotal(
			@Param("memberId") Long memberId,
			@Param("status") ShipmentStatus status,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	interface DriverMonthlyTotal {
		Long getCompletedCount();

		Long getTotalFare();
	}

	// 기사별 월간 배송 완료 운임 상세 내역
	@Query("select h.shipment.id as shipmentId, "
			+ "h.shipment.cargoName as cargoName, "
			+ "h.shipment.fare as fare, "
			+ "h.changedAt as completedAt "
			+ "from ShipmentStatusHistory h "
            + "where h.toStatus = :status "
            + "and h.shipment.status = :status "
            + "and (exists (select 1 from Dispatch d where d.shipment = h.shipment "
            + "and d.active = true and d.driver.member.id = :memberId) "
            + "or (h.changedBy.id = :memberId and not exists "
            + "(select 1 from Dispatch d where d.shipment = h.shipment and d.active = true))) "
			+ "and h.changedAt >= :from and h.changedAt < :to "
			+ "order by h.changedAt desc")
	List<DriverMonthlyFareItem> findDriverMonthlyFareItems(
			@Param("memberId") Long memberId,
			@Param("status") ShipmentStatus status,
			@Param("from") LocalDateTime from,
			@Param("to") LocalDateTime to);

	interface DriverMonthlyFareItem {
		Long getShipmentId();

		String getCargoName();

		Integer getFare();

		LocalDateTime getCompletedAt();
	}
}
