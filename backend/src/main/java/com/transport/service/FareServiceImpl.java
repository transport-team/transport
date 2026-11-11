package com.transport.service;

import java.util.Comparator;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import com.transport.domain.CargoType;
import com.transport.domain.VehicleType;
import com.transport.repository.CargoTypeRepository;
import com.transport.service.DistanceService.Route;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FareServiceImpl implements FareService {

	private final VehicleTypeService vehicleTypeService;
	private final CargoTypeRepository cargoTypeRepository;
	private final DistanceService distanceService;
	private final FareCalculator fareCalculator;

	@Override
	public FareCalculateResponseDTO calculateFare(FareCalculateRequestDTO requestDTO) {
		VehicleType type = vehicleTypeService.findById(requestDTO.getVehicleTypeId())
				.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 차종입니다."));
		CargoType cargo = cargoTypeRepository.findById(requestDTO.getCargoTypeId())
				.orElseThrow(() -> new EntityNotFoundException("존재하지 않는 화물 유형입니다."));

		// 거리는 서버가 계산, 할증은 pickupAt으로 서버가 판단 (화면 값은 믿지 않는다)
		Route route = distanceService.findRoute(requestDTO.getOriginAddress(), requestDTO.getDestAddress());
		int weightKg = requestDTO.getCargoWeightKg();

		int totalFare = fareCalculator.calculate(type, cargo, route.distanceKm(), weightKg, requestDTO.getPickupAt());

		int baseFare = type.getBaseFare();
		int distanceFare = fareCalculator.distanceFare(type, route.distanceKm());
		int cargoFare = cargo.getExtraFee();
		int surchargeFare = totalFare - (baseFare + distanceFare + cargoFare);

		// 무게 기준 자동 추천: 무게를 실을 수 있는 가장 작은 차종 (없으면 문의 안내)
		Optional<VehicleType> recommended = vehicleTypeService.findAllEntities().stream()
				.filter(v -> v.getMaxLoadKg() >= weightKg)
				.min(Comparator.comparingInt(VehicleType::getMaxLoadKg));

		log.info("Fare calculated: distance={}km, weight={}kg, total={}", route.distanceKm(), weightKg, totalFare);

		return FareCalculateResponseDTO.builder()
				.baseFare(baseFare)
				.distanceFare(distanceFare)
				.cargoFare(cargoFare)
				.surchargeFare(surchargeFare)
				.totalFare(totalFare)
				.distanceKm(route.distanceKm())
				.durationMin(route.durationMin())
				.originLat(route.originLat())
				.originLng(route.originLng())
				.destLat(route.destLat())
				.destLng(route.destLng())
				.recommendedVehicleTypeId(recommended.map(VehicleType::getId).orElse(null))
				.inquiryRequired(recommended.isEmpty())
				.build();
	}
}
