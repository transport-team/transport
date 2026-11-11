package com.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Dispatch;
import com.transport.domain.Shipment;

public interface DispatchRepository extends JpaRepository<Dispatch, Long> {

    // 해당 배송에 현재 유효한 배차가 있는지 확인
    boolean existsByShipmentAndActiveTrue(Shipment shipment);

    // 해당 배송의 현재 유효한 배차 조회 (getActiveDriver용 — 5-5)
    Optional<Dispatch> findByShipmentIdAndActiveTrue(Long shipmentId);
    
	// 기사별 현재 유효한 배차 목록 조회 (운송 시간 중복 검사에 사용)
	List<Dispatch> findByDriver_IdAndActiveTrue(Long driverId);

	// 특정 차량에 현재 유효한 배차가 있는지 확인
	boolean existsByVehicle_IdAndActiveTrue(Long vehicleId);
}
