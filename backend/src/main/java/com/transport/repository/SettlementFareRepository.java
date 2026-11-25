package com.transport.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;

// 정산에는 상태 변경자가 아닌 실제 활성 배차 기사 ID를 기준으로 사용한다.
public interface SettlementFareRepository extends JpaRepository<ShipmentStatusHistory, Long> {
    @Query("select h.shipment.id as shipmentId, h.shipment.fare as fare, "
            + "h.changedAt as completedAt "
            + "from ShipmentStatusHistory h join Dispatch d on d.shipment = h.shipment "
            + "where h.toStatus = :status and h.shipment.status = :status "
            + "and d.active = true and d.driver.id = :driverId "
            + "and h.changedAt >= :from and h.changedAt < :to "
            + "order by h.changedAt asc")
    List<SettlementFareItem> findCompletedForDriver(
            @Param("driverId") Long driverId,
            @Param("status") ShipmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    // 같은 조건을 모든 기사 한 번에 — 월 정산 목록 · 추이에서 기사마다 부르던 N+1 정리 (11-25)
    @Query("select d.driver.id as driverId, h.shipment.id as shipmentId, h.shipment.fare as fare, "
            + "h.changedAt as completedAt "
            + "from ShipmentStatusHistory h join Dispatch d on d.shipment = h.shipment "
            + "where h.toStatus = :status and h.shipment.status = :status "
            + "and d.active = true "
            + "and h.changedAt >= :from and h.changedAt < :to "
            + "order by h.changedAt asc")
    List<SettlementDriverFareItem> findCompletedForAllDrivers(
            @Param("status") ShipmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    interface SettlementFareItem {
        Long getShipmentId();
        Integer getFare();
        LocalDateTime getCompletedAt();
    }

    interface SettlementDriverFareItem {
        Long getDriverId();
        Long getShipmentId();
        Integer getFare();
        LocalDateTime getCompletedAt();
    }
}
