package com.transport.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.VehicleType;
import com.transport.dto.VehicleTypeRequestDTO;
import com.transport.dto.VehicleTypeResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.VehicleTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleTypeServiceImpl implements VehicleTypeService {

	private final VehicleTypeRepository vehicleTypeRepository;

	@Override
	public List<VehicleTypeResponseDTO> getVehicleTypes() {

		return vehicleTypeRepository.findAll().stream().filter(VehicleType::isActive).map(this::toDTO).toList();
	}

	@Override
	public List<VehicleTypeResponseDTO> getAllVehicleTypesForAdmin() {
		return vehicleTypeRepository.findAll().stream().map(this::toDTO).toList();
	}

	@Override
	@Transactional
	public VehicleTypeResponseDTO registerVehicleType(VehicleTypeRequestDTO request) {
		String code = request.getCode().trim();
		if (vehicleTypeRepository.existsByCode(code)) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "이미 사용 중인 차종 코드입니다.");
		}
		VehicleType saved = vehicleTypeRepository.save(VehicleType.builder()
				.code(code)
				.name(request.getName().trim())
				.maxLoadKg(request.getMaxLoadKg())
				.baseFare(request.getBaseFare())
				.baseDistanceKm(request.getBaseDistanceKm())
				.perKmFare(request.getPerKmFare())
				.nightRate(request.getNightRate())
				.weekendRate(request.getWeekendRate())
				.heavyRate(request.getHeavyRate())
				.active(request.getActive())
				.build());
		return toDTO(saved);
	}

	@Override
	@Transactional
	public VehicleTypeResponseDTO updateVehicleType(Long id, VehicleTypeRequestDTO request) {
		VehicleType type = vehicleTypeRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 차종입니다."));
		if (!type.getCode().equals(request.getCode().trim())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "기존 차종 코드는 변경할 수 없습니다.");
		}
		type.updateFareSettings(request.getName().trim(), request.getMaxLoadKg(),
				request.getBaseFare(), request.getBaseDistanceKm(), request.getPerKmFare(),
				request.getNightRate(), request.getWeekendRate(), request.getHeavyRate());
		type.changeActive(request.getActive());
		return toDTO(type);
	}

	@Override
	@Transactional
	public VehicleTypeResponseDTO changeVehicleTypeActive(Long id, boolean active) {
		VehicleType type = vehicleTypeRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "존재하지 않는 차종입니다."));
		type.changeActive(active);
		return toDTO(type);
	}

	private VehicleTypeResponseDTO toDTO(VehicleType vehicleType) {

		return VehicleTypeResponseDTO.builder()
				.id(vehicleType.getId())
				.code(vehicleType.getCode())
				.name(vehicleType.getName())
				.maxLoadKg(vehicleType.getMaxLoadKg())
				.baseFare(vehicleType.getBaseFare())
				.baseDistanceKm(vehicleType.getBaseDistanceKm())
				.perKmFare(vehicleType.getPerKmFare())
				.nightRate(vehicleType.getNightRate())
				.weekendRate(vehicleType.getWeekendRate())
				.heavyRate(vehicleType.getHeavyRate())
				.active(vehicleType.isActive())
				.build();
	}

	@Override
	public Optional<VehicleType> findById(Long id) {

		return vehicleTypeRepository.findById(id);
	}

	@Override
	public List<VehicleType> findAllEntities() {
		return vehicleTypeRepository.findAll();
	}
}
