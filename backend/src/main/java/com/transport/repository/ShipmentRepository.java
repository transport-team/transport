
package com.transport.repository;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    @Query("""
            select s from Shipment s
            where s.customer.id = :customerId
              and (:status is null or s.status = :status)
              and (:from is null or s.createdAt >= :from)
              and (:to is null or s.createdAt < :to)
            """)
    Page<Shipment> findMyShipments(
            @Param("customerId") Long customerId,
            @Param("status") ShipmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable);

    // 기사: 배차 대기 중인 운송 요청 목록 조회 (상차 예정일 빠른 순)
    List<Shipment> findByStatusOrderByPickupAtAsc(ShipmentStatus status);

    // 관리자 대시보드: 상태별 배송 건수 (상태마다 따로 세지 않고 쿼리 한 번으로, 배송이 없는 상태는 빠진다)
    @Query("select s.status as status, count(s) as shipmentCount from Shipment s group by s.status")
    List<StatusCount> countGroupByStatus();

    // 상태별 건수 결과
    interface StatusCount {
        ShipmentStatus getStatus();

        Long getShipmentCount();
    }
}
