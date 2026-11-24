package com.transport.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.VehicleTypeResponseDTO;
import com.transport.service.VehicleTypeService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/vehicle-types")
public class VehicleTypeController {

	private final VehicleTypeService vehicleTypeService;

	@GetMapping
	public List<VehicleTypeResponseDTO> getVehicleTypes() {

		return vehicleTypeService.getVehicleTypes();
	}
}
