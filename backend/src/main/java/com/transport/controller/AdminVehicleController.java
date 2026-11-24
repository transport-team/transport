package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.VehicleRequestDTO;
import com.transport.dto.VehicleResponseDTO;
import com.transport.service.VehicleService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/vehicles")
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleController {

	private final VehicleService vehicleService;

	@GetMapping
	public PageResponseDTO<VehicleResponseDTO> getVehicles(@Valid PageRequestDTO pageRequestDTO) {

		return vehicleService.getVehicles(pageRequestDTO);
	}

	// 관리자 차량 등록
	@PostMapping
	public Long registerVehicle(@Valid @RequestBody VehicleRequestDTO vehicleRequestDTO) {

		return vehicleService.registerVehicle(vehicleRequestDTO);
	}

	// 관리자 차량 정보 수정
	@PutMapping("/{id}")
	public Long updateVehicle(@PathVariable("id") Long id, @Valid @RequestBody VehicleRequestDTO vehicleRequestDTO) {

		return vehicleService.updateVehicle(id, vehicleRequestDTO);
	}

	// 관리자 차량 비활성화
	@DeleteMapping("/{id}")
	public Long deactivateVehicle(@PathVariable("id") Long id) {

		return vehicleService.deactivateVehicle(id);
	}

}
