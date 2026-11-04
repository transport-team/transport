package com.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Driver;
import com.transport.domain.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {

	boolean existsByPlateNo(String plateNo);

	// 이미 다른 차량에 배정된 기사인지 확인
	boolean existsByDriver(Driver driver);

	// 수정 중인 차량을 제외하고 차량번호 중복 확인
	boolean existsByPlateNoAndIdNot(String plateNo, Long id);

	// 수정 중인 차량을 제외하고 담당 기사 중복 배정 확인
	boolean existsByDriverAndIdNot(Driver driver, Long id);

}
