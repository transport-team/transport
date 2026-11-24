// CargoTypeServiceImpl.java
package com.transport.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.dto.CargoTypeRequestDTO;
import com.transport.dto.CargoTypeResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.CargoTypeRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CargoTypeServiceImpl implements CargoTypeService {

	private final CargoTypeRepository cargoTypeRepository;

	@Override
	public List<CargoTypeResponseDTO> getActiveCargoTypes() {
		return cargoTypeRepository.findAll(Sort.by("id").ascending()).stream()
				.filter(CargoType::isActive)
				.map(this::toDTO)
				.toList();
	}

	@Override
	public List<CargoTypeResponseDTO> getAllCargoTypes() {
		return cargoTypeRepository.findAll(Sort.by("id").ascending()).stream()
				.map(this::toDTO)
				.toList();
	}

	@Override
	@Transactional
	public CargoTypeResponseDTO registerCargoType(CargoTypeRequestDTO requestDTO) {
		String trimmedName = requestDTO.getName().trim();
		if (cargoTypeRepository.existsByName(trimmedName)) {
			throw new CustomException(ErrorCode.DUPLICATE_CARGO_TYPE_NAME);
		}

		CargoType saved = cargoTypeRepository.save(CargoType.builder()
				.name(trimmedName)
				.extraFee(requestDTO.getExtraFee())
				.handlingNote(requestDTO.getHandlingNote())
				.active(requestDTO.isActive())
				.build());

		return toDTO(saved);
	}

	@Override
	@Transactional
	public CargoTypeResponseDTO updateCargoType(Long id, CargoTypeRequestDTO requestDTO) {
		CargoType cargoType = cargoTypeRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.CARGO_TYPE_NOT_FOUND));

		String trimmedName = requestDTO.getName().trim();
		if (cargoTypeRepository.existsByNameAndIdNot(trimmedName, id)) {
			throw new CustomException(ErrorCode.DUPLICATE_CARGO_TYPE_NAME);
		}

		cargoType.update(trimmedName, requestDTO.getExtraFee(), requestDTO.getHandlingNote(), requestDTO.isActive());
		return toDTO(cargoType);
	}

	@Override
	@Transactional
	public CargoTypeResponseDTO changeCargoTypeActive(Long id, boolean active) {
		CargoType cargoType = cargoTypeRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.CARGO_TYPE_NOT_FOUND));

		cargoType.changeActive(active);
		return toDTO(cargoType);
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