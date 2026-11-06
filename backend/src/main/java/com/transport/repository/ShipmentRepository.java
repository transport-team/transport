package com.transport.repository;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    // 향후 회원별 배송 내역 조회 기능 등을 위해 추가할 메서드 예시
    // List<Shipment> findAllByCustomerId(Long customerId);

    // 관리자 대시보드: 상태별 배송 건수 (상태마다 따로 세지 않고 쿼리 한 번으로, 배송이 없는 상태는 빠진다)
    @Query("select s.status as status, count(s) as shipmentCount from Shipment s group by s.status")
    List<StatusCount> countGroupByStatus();

    // 상태별 건수 결과
    interface StatusCount {
        ShipmentStatus getStatus();

        Long getShipmentCount();
    }
}
