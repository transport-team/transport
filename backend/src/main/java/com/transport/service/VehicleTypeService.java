package com.transport.service;

import java.util.List;
import java.util.Optional;

import com.transport.domain.VehicleType;
import com.transport.dto.VehicleTypeRequestDTO;
import com.transport.dto.VehicleTypeResponseDTO;

public interface VehicleTypeService {

	List<VehicleTypeResponseDTO> getVehicleTypes();

	List<VehicleTypeResponseDTO> getAllVehicleTypesForAdmin();

	VehicleTypeResponseDTO registerVehicleType(VehicleTypeRequestDTO request);

	VehicleTypeResponseDTO updateVehicleType(Long id, VehicleTypeRequestDTO request);

	VehicleTypeResponseDTO changeVehicleTypeActive(Long id, boolean active);

	Optional<VehicleType> findById(Long id);

	List<VehicleType> findAllEntities();

}
