package com.transport.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.DriverVehicleResponseDTO;
import com.transport.dto.DriverVehicleStatusRequestDTO;
import com.transport.security.LoginMember;
import com.transport.service.VehicleRegistrationService;

import lombok.RequiredArgsConstructor;

// 기사 본인 차량 조회 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/vehicles")
@PreAuthorize("hasRole('DRIVER')")
public class DriverVehicleController {

    private final VehicleRegistrationService vehicleRegistrationService;

    // 로그인한 기사의 등록 차량 목록 조회
    @GetMapping
    public List<DriverVehicleResponseDTO> getMyVehicles(
            @AuthenticationPrincipal LoginMember loginMember) {

        return vehicleRegistrationService.getMyVehicles(loginMember.getId());
    }

    // 로그인한 기사의 본인 차량 상태 변경
    @PatchMapping("/{vehicleId}/status")
    public DriverVehicleResponseDTO changeMyVehicleStatus(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable("vehicleId") Long vehicleId,
            @RequestBody DriverVehicleStatusRequestDTO requestDTO) {

        return vehicleRegistrationService.changeMyVehicleStatus(
                loginMember.getId(), vehicleId, requestDTO.status());
    }
}
