package com.transport.repository;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {

    // 관리자 배차 취소 시 배송 건 비관적 락 조회
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Shipment s where s.id = :id")
    Optional<Shipment> findByIdForUpdate(@Param("id") Long id);

    @EntityGraph(attributePaths = { "vehicleType", "cargoType" })
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

    // 기사: 배차 대기 중인 운송 요청 목록 조회 (등록일 최신순)
    List<Shipment> findByStatusOrderByCreatedAtDescIdDesc(ShipmentStatus status);

    // 관리자 대시보드: 상태별 배송 건수 (상태마다 따로 세지 않고 쿼리 한 번으로, 배송이 없는 상태는 빠진다)
    @Query("select s.status as status, count(s) as shipmentCount from Shipment s group by s.status")
    List<StatusCount> countGroupByStatus();

    // 상태별 건수 결과
    interface StatusCount {
        ShipmentStatus getStatus();

        Long getShipmentCount();
    }

    // 관리자 배송 통합 검색: customer, vehicleType, cargoType을 EntityGraph로 한 번에 조회 (N+1 방지)
    @EntityGraph(attributePaths = { "customer", "vehicleType", "cargoType" })
    @Query("""
            select distinct s from Shipment s
            left join Dispatch d on d.shipment = s and d.active = true
            left join d.driver dr
            left join dr.member dm
            where (:status is null or s.status = :status)
              and (:from is null or s.createdAt >= :from)
              and (:to is null or s.createdAt < :to)
              and (:keyword is null or :keyword = ''
                   or str(s.id) = :keyword
                   or lower(s.cargoName) like lower(concat('%', :keyword, '%'))
                   or lower(s.originAddress) like lower(concat('%', :keyword, '%'))
                   or lower(s.destAddress) like lower(concat('%', :keyword, '%'))
                   or lower(s.customer.name) like lower(concat('%', :keyword, '%'))
                   or lower(s.customer.email) like lower(concat('%', :keyword, '%'))
                   or lower(s.receiverName) like lower(concat('%', :keyword, '%'))
                   or (dm.name is not null and lower(dm.name) like lower(concat('%', :keyword, '%')))
                   or (dr.id is not null and str(dr.id) = :keyword))
            """)
    Page<Shipment> searchAdminShipments(
            @Param("status") ShipmentStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            @Param("keyword") String keyword,
            Pageable pageable);
}