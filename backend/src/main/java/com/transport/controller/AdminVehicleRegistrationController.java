package com.transport.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.VehicleRegistrationRejectDTO;
import com.transport.dto.VehicleRegistrationResponseDTO;
import com.transport.service.VehicleRegistrationService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 관리자 차량 등록 신청 관리 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/vehicle-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleRegistrationController {

    private final VehicleRegistrationService vehicleRegistrationService;

    // 승인 대기 중인 차량 등록 신청 목록 조회
    @GetMapping
    public List<VehicleRegistrationResponseDTO> getPendingRequests() {
        return vehicleRegistrationService.getPendingRequests();
    }
    
    // 관리자: 차량 등록 신청 승인
    @PatchMapping("/{requestId}/approve")
    public void approve(@PathVariable("requestId") Long requestId) {

        vehicleRegistrationService.approve(requestId);
    }
    
    // 관리자: 차량 등록 신청 거절
    @PatchMapping("/{requestId}/reject")
    public void reject(
            @PathVariable("requestId") Long requestId,
            @Valid @RequestBody VehicleRegistrationRejectDTO requestDTO) {

        vehicleRegistrationService.reject(
                requestId,
                requestDTO.getRejectionReason()
        );
    }

}