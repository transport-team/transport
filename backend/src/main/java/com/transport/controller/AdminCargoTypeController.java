package com.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.CargoTypeRequestDTO;
import com.transport.dto.CargoTypeResponseDTO;
import com.transport.service.CargoTypeService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin CargoType API", description = "관리자 화물 유형 관리 API (ADM-08)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/cargo-types")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCargoTypeController {

	private final CargoTypeService cargoTypeService;

	@Operation(summary = "전체 화물 유형 목록 조회 (비활성 포함)")
	@GetMapping
	public List<CargoTypeResponseDTO> list() {
		return cargoTypeService.getAllCargoTypes();
	}

	@Operation(summary = "화물 유형 신규 등록")
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CargoTypeResponseDTO register(@Valid @RequestBody CargoTypeRequestDTO requestDTO) {
		return cargoTypeService.registerCargoType(requestDTO);
	}

	@Operation(summary = "화물 유형 수정")
	@PutMapping("/{id}")
	public CargoTypeResponseDTO update(@PathVariable Long id, @Valid @RequestBody CargoTypeRequestDTO requestDTO) {
		return cargoTypeService.updateCargoType(id, requestDTO);
	}

	@Operation(summary = "화물 유형 활성/비활성 전환")
	@PatchMapping("/{id}/active")
	public CargoTypeResponseDTO changeActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
		boolean active = Boolean.TRUE.equals(body.get("active"));
		return cargoTypeService.changeCargoTypeActive(id, active);
	}
}