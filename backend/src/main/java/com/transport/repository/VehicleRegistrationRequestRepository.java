package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Driver;
import com.transport.domain.VehicleRegistrationRequest;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.VehicleRegistrationStatus;

public interface VehicleRegistrationRequestRepository
        extends JpaRepository<VehicleRegistrationRequest, Long> {

    // 동일 기사가 같은 차종으로 승인 대기 중인 신청이 있는지 확인
    boolean existsByDriverAndVehicleTypeAndStatus(
            Driver driver,
            VehicleType vehicleType,
            VehicleRegistrationStatus status
    );

    // 동일 차량번호로 승인 대기 중인 신청이 있는지 확인
    boolean existsByPlateNoAndStatus(
            String plateNo,
            VehicleRegistrationStatus status
    );

    // 관리자: 승인 대기 중인 차량 신청을 최신순으로 조회
    List<VehicleRegistrationRequest> findByStatusOrderByCreatedAtDesc(
            VehicleRegistrationStatus status
    );
}