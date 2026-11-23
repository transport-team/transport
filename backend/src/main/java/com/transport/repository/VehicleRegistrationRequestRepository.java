package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

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

    // 기사 본인의 차량 등록 신청 내역 (관리자 심사 완료 포함)
    @EntityGraph(attributePaths = {"vehicleType"})
    List<VehicleRegistrationRequest> findByDriver_Member_IdOrderByCreatedAtDescIdDesc(Long memberId);

    // 기사 철회/관리자 처리 충돌 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from VehicleRegistrationRequest r where r.id = :requestId")
    Optional<VehicleRegistrationRequest> findForReview(@Param("requestId") Long requestId);

    // 관리자: 처리 완료 건을 포함해 전체 신청을 신청일 최신순으로 조회
    @EntityGraph(attributePaths = {"driver", "driver.member", "vehicleType"})
    List<VehicleRegistrationRequest> findAllByOrderByCreatedAtDescIdDesc();

    // 관리자: 승인 대기 중인 차량 신청을 최신순으로 조회
    List<VehicleRegistrationRequest> findByStatusOrderByCreatedAtDesc(
            VehicleRegistrationStatus status
    );
}