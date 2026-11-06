package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
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

    // 로그인한 기사의 차량 등록 신청
    @PostMapping
    public Long apply(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody VehicleRegistrationRequestDTO requestDTO) {

        return vehicleRegistrationService.apply(loginMember.getId(), requestDTO);
    }
}