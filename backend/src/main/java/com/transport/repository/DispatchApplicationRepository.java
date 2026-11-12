package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Dispatch;
import com.transport.domain.DispatchApplication;
import com.transport.domain.Driver;
import com.transport.domain.Shipment;
import com.transport.domain.enums.DispatchApplicationStatus;

public interface DispatchApplicationRepository extends JpaRepository<DispatchApplication, Long> {

        // 동일 기사가 해당 배송에 이미 승인 대기 중인 지원을 했는지 확인
        boolean existsByShipmentAndDriverAndStatus(
                        Shipment shipment,
                        Driver driver,
                        DispatchApplicationStatus status);

        // 물주: 해당 배송에 지원한 기사 목록 조회 (지원한 순서대로)
        List<DispatchApplication> findByShipmentAndStatusOrderByCreatedAtAsc(
                        Shipment shipment,
                        DispatchApplicationStatus status);

        // 기사별 배차 신청 대기 목록 조회 (운송 시간 중복 경고에 사용)
        List<DispatchApplication> findByDriver_IdAndStatus(
                        Long driverId,
                        DispatchApplicationStatus status);

        // 기사: 본인의 전체 신청 기록 조회 (최근 변경 순)
        List<DispatchApplication> findByDriver_IdOrderByUpdatedAtDesc(
                        Long driverId);

}
