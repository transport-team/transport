package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.DriverDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.DriverStatusRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.DriverService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 관리자 기사 관리 API: 목록 · 승인 · 중지 (팀원 2 도메인 — 팀원 2 부재 기간에 팀원 1이 작성)
@RestController
@RequestMapping("/api/admin/drivers")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")  // 권한 전환일 전이라도 관리자만
public class AdminDriverController {

	private final DriverService driverService;

	@GetMapping
	public PageResponseDTO<DriverDTO> list(@Valid DriverSearchDTO driverSearchDTO) {
		return driverService.getDrivers(driverSearchDTO);
	}

	@PatchMapping("/{id}/status")
	public DriverDTO changeStatus(@PathVariable Long id, @Valid @RequestBody DriverStatusRequestDTO driverStatusRequestDTO) {
		return driverService.changeStatus(id, driverStatusRequestDTO.getStatus());
	}
}
