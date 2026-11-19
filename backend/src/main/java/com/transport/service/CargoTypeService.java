// CargoTypeService.java
package com.transport.service;

import java.util.List;

import com.transport.dto.CargoTypeRequestDTO;
import com.transport.dto.CargoTypeResponseDTO;

public interface CargoTypeService {

	List<CargoTypeResponseDTO> getActiveCargoTypes();

	List<CargoTypeResponseDTO> getAllCargoTypes();

	CargoTypeResponseDTO registerCargoType(CargoTypeRequestDTO requestDTO);

	CargoTypeResponseDTO updateCargoType(Long id, CargoTypeRequestDTO requestDTO);

	CargoTypeResponseDTO changeCargoTypeActive(Long id, boolean active);
}