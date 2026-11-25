package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.Dispatch;
import com.transport.domain.DispatchApplication;
import com.transport.domain.Driver;
import com.transport.domain.Shipment;
import com.transport.domain.enums.DispatchApplicationStatus;

public interface DispatchApplicationRepository extends JpaRepository<DispatchApplication, Long> {

        // 로그인 고객의 배차 대기 배송별 지원자 수 (거절/취소된 신청은 제외)
        @Query("select a.shipment.id, count(a.id) from DispatchApplication a "
                + "where a.shipment.customer.id = :customerId "
                + "and a.shipment.status = :shipmentStatus "
                + "and a.status = :applicationStatus "
                + "group by a.shipment.id")
        List<Object[]> countPendingByCustomer(
                @Param("customerId") Long customerId,
                @Param("shipmentStatus") com.transport.domain.enums.ShipmentStatus shipmentStatus,
                @Param("applicationStatus") DispatchApplicationStatus applicationStatus);

        boolean existsByVehicle_IdAndStatus(Long vehicleId, DispatchApplicationStatus status);


        // 동일 기사가 해당 배송에 이미 승인 대기 중인 지원을 했는지 확인
        boolean existsByShipmentAndDriverAndStatus(
                        Shipment shipment,
                        Driver driver,
                        DispatchApplicationStatus status);

        // 물주: 해당 배송에 지원한 기사 목록 조회 (지원한 순서대로)
        List<DispatchApplication> findByShipmentAndStatusOrderByCreatedAtAsc(
                        Shipment shipment,
                        DispatchApplicationStatus status);

        // 기사별 배차 신청 대기 목록 조회 (운송 시간 중복 경고에 사용) — 배송을 함께 가져온다(N+1 정리, 11-25)
        @EntityGraph(attributePaths = {"shipment"})
        List<DispatchApplication> findByDriver_IdAndStatus(
                        Long driverId,
                        DispatchApplicationStatus status);

        // 기사: 본인의 전체 신청 기록 조회 (최근 변경 순) — 지난 내역에서 배송 상태로 거르므로 배송을 함께 가져온다
        @EntityGraph(attributePaths = {"shipment"})
        List<DispatchApplication> findByDriver_IdOrderByUpdatedAtDesc(
                        Long driverId);

}
