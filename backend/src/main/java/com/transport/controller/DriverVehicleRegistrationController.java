package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import java.util.List;
import com.transport.dto.VehicleRegistrationResponseDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.VehicleRegistrationRequestDTO;
import com.transport.security.LoginMember;
import com.transport.service.VehicleRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 기사 차량 등록 신청 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/vehicles/requests")
@PreAuthorize("hasRole('DRIVER')")
public class DriverVehicleRegistrationController {

    private final VehicleRegistrationService vehicleRegistrationService;

    // 기사 본인의 신청 내역 조회 (승인/거절/철회 포함)
    @GetMapping
    public List<VehicleRegistrationResponseDTO> myRequests(
            @AuthenticationPrincipal LoginMember loginMember) {
        return vehicleRegistrationService.getMyRequests(loginMember.getId());
    }

    // 승인 대기 중인 본인 신청 철회
    @DeleteMapping("/{requestId}")
    public void withdraw(@AuthenticationPrincipal LoginMember loginMember,
                         @PathVariable Long requestId) {
        vehicleRegistrationService.withdrawRequest(loginMember.getId(), requestId);
    }

    // 로그인한 기사의 차량 등록 신청
    @PostMapping
    public Long apply(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody VehicleRegistrationRequestDTO requestDTO) {

        return vehicleRegistrationService.apply(loginMember.getId(), requestDTO);
    }
}