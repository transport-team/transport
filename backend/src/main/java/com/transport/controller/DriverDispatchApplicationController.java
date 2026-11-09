package com.transport.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.DispatchApplyRequestDTO;
import com.transport.dto.DriverShipmentListResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.DispatchApplicationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 기사 운송 요청 지원 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/shipments")
@PreAuthorize("hasRole('DRIVER')")
public class DriverDispatchApplicationController {

    private final DispatchApplicationService dispatchApplicationService;

    // 로그인한 기사가 원하는 운송 요청에 지원
    @PostMapping("/{shipmentId}/applications")
    public Long apply(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable("shipmentId") Long shipmentId,
            @Valid @RequestBody DispatchApplyRequestDTO requestDTO) {

        return dispatchApplicationService.apply(
                loginMember.getId(),
                shipmentId,
                requestDTO.getVehicleId()
        );
    }

	// 기사: 배차 대기 중인 운송 요청 목록 조회
	@GetMapping
	public ResponseEntity<List<DriverShipmentListResponseDTO>> getAvailableShipments(
			@AuthenticationPrincipal LoginMember loginMember) {

		List<DriverShipmentListResponseDTO> shipments =
				dispatchApplicationService.getAvailableShipments(loginMember.getId());

		return ResponseEntity.ok(shipments);
	}


}
