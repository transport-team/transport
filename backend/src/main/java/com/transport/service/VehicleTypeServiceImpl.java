package com.transport.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.VehicleType;
import com.transport.dto.VehicleTypeResponseDTO;
import com.transport.repository.VehicleTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleTypeServiceImpl implements VehicleTypeService {

	private final VehicleTypeRepository vehicleTypeRepository;

	@Override
	public List<VehicleTypeResponseDTO> getVehicleTypes() {

		return vehicleTypeRepository.findAll().stream().map(this::toDTO).toList();
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
