package com.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Driver;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleType;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

    boolean existsByPlateNo(String plateNo);

    // 동일 기사가 동일 차종의 차량을 이미 등록했는지 확인
    boolean existsByDriverAndVehicleType(Driver driver, VehicleType vehicleType);

    // 수정 중인 차량을 제외하고 차량번호 중복 확인
    boolean existsByPlateNoAndIdNot(String plateNo, Long id);

    // 수정 중인 차량을 제외하고 동일 기사 + 동일 차종 중복 확인
    boolean existsByDriverAndVehicleTypeAndIdNot(Driver driver, VehicleType vehicleType, Long id);

}