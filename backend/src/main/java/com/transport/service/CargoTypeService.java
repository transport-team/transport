package com.transport.service;

import java.util.List;

import com.transport.dto.CargoTypeResponseDTO;

public interface CargoTypeService {

	List<CargoTypeResponseDTO> getActiveCargoTypes();
}
