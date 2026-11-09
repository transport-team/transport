package com.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.dto.CargoTypeResponseDTO;
import com.transport.repository.CargoTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CargoTypeServiceImpl implements CargoTypeService {

	private final CargoTypeRepository cargoTypeRepository;

	@Override
	public List<CargoTypeResponseDTO> getActiveCargoTypes() {
		return cargoTypeRepository.findAll().stream()
				.filter(CargoType::isActive)
				.map(this::toDTO)
				.toList();
	}

	private CargoTypeResponseDTO toDTO(CargoType cargoType) {
		return CargoTypeResponseDTO.builder()
				.id(cargoType.getId())
				.name(cargoType.getName())
				.extraFee(cargoType.getExtraFee())
				.handlingNote(cargoType.getHandlingNote())
				.active(cargoType.isActive())
				.build();
	}
}
