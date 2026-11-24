package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import com.transport.domain.Driver;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.VehicleStatus;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlateNo(String plateNo);

    // 동일 기사가 동일 차종의 차량을 이미 등록했는지 확인
    boolean existsByDriverAndVehicleType(Driver driver, VehicleType vehicleType);

    // 수정 중인 차량을 제외하고 차량번호 중복 확인
    boolean existsByPlateNoAndIdNot(String plateNo, Long id);

    // 수정 중인 차량을 제외하고 동일 기사 + 동일 차종 중복 확인
    boolean existsByDriverAndVehicleTypeAndIdNot(Driver driver, VehicleType vehicleType, Long id);

    // 관리자 기사별 차량 보기 (등록일 최신순, 비활성 차량도 포함)
    @EntityGraph(attributePaths = "vehicleType")
    List<Vehicle> findByDriver_IdOrderByCreatedAtDescIdDesc(Long driverId);

    // 삭제 신청과 관리자 승인에서 같은 차량의 동시 수정 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehicle v where v.id = :id")
    Optional<Vehicle> findForUpdate(@Param("id") Long id);

    // 비활성 차량은 기사 화면에서 숨기되 DB 이력은 보존
    List<Vehicle> findByDriver_IdAndStatusNot(Long driverId, VehicleStatus status);

    // 기사가 소유한 차량 목록 조회 (배차 신청 가능 여부 판단)
    List<Vehicle> findByDriver_Id(Long driverId);

}