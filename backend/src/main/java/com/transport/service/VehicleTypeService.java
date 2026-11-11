package com.transport.service;

import java.util.List;
import java.util.Optional;

import com.transport.domain.VehicleType;
import com.transport.dto.VehicleTypeResponseDTO;

public interface VehicleTypeService {

	List<VehicleTypeResponseDTO> getVehicleTypes();

	Optional<VehicleType> findById(Long id);

	List<VehicleType> findAllEntities();

}
