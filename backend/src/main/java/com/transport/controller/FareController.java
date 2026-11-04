package com.transport.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import com.transport.service.FareService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Fare API", description = "운임 산정 API")
@RestController
@RequestMapping("/api/fares")
@RequiredArgsConstructor
public class FareController {

	private final FareService fareService;

	@Operation(summary = "예상 운임 계산", description = "출발지, 도착지, 무게, 화물 종류, 상차 일시를 바탕으로 예상 운임을 계산합니다.")
	@PreAuthorize("hasRole('CUSTOMER')")
	@PostMapping("/estimate")
	public ResponseEntity<FareCalculateResponseDTO> estimate(
			@Valid @RequestBody FareCalculateRequestDTO requestDTO) {
		return ResponseEntity.ok(fareService.calculateFare(requestDTO));
	}
}
