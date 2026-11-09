package com.transport.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.CargoTypeResponseDTO;
import com.transport.service.CargoTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Cargo Type API", description = "화물 유형 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/cargo-types")
public class CargoTypeController {

	private final CargoTypeService cargoTypeService;

	@Operation(summary = "활성 화물 유형 목록 조회")
	@GetMapping
	public List<CargoTypeResponseDTO> getCargoTypes() {
		return cargoTypeService.getActiveCargoTypes();
	}
}
