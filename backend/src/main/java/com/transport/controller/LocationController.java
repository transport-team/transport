package com.transport.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.LocationRequestDTO;
import com.transport.dto.LocationResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.LocationService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 위치 API (7-1 — 팀원 4)
@RestController
@RequestMapping("/api/shipments/{id}/locations")
@RequiredArgsConstructor
@Log4j2
public class LocationController {

	private final LocationService locationService;

	@Operation(summary = "위치 전송 (기사)", description = "배정 기사 + IN_TRANSIT일 때만. 아니면 403/400")
	@PreAuthorize("hasRole('DRIVER')")
	@PostMapping
	public ResponseEntity<LocationResponseDTO> sendLocation(
			@PathVariable Long id,
			@AuthenticationPrincipal LoginMember loginMember,
			@Valid @RequestBody LocationRequestDTO requestDTO) {

		LocationResponseDTO response = locationService.sendLocation(
				id, loginMember.getId(), requestDTO.getLat(), requestDTO.getLng());
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "마지막 위치", description = "기록이 없으면 204 (아직 안 보낸 것 — 정상)")
	@GetMapping("/latest")
	public ResponseEntity<?> getLatestLocation(@PathVariable Long id) {
		LocationResponseDTO response = locationService.getLatestLocation(id);
		if (response == null) {
			return ResponseEntity.noContent().build();
		}
		return ResponseEntity.ok(response);
	}
}