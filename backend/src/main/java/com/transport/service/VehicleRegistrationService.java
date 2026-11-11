package com.transport.service;

import java.util.List;

import com.transport.domain.enums.VehicleStatus;
import com.transport.dto.DriverVehicleResponseDTO;
import com.transport.dto.VehicleRegistrationRequestDTO;
import com.transport.dto.VehicleRegistrationResponseDTO;

public interface VehicleRegistrationService {

    // 로그인한 기사의 차량 등록 신청
    Long apply(Long memberId, VehicleRegistrationRequestDTO requestDTO);

    // 관리자: 승인 대기 중인 차량 등록 신청 목록 조회
    List<VehicleRegistrationResponseDTO> getPendingRequests();

    // 관리자: 차량 등록 신청 승인
    void approve(Long requestId);

    // 관리자: 차량 등록 신청 거절
    void reject(Long requestId, String rejectionReason);

    // 기사: 본인 차량 목록 조회
    List<DriverVehicleResponseDTO> getMyVehicles(Long memberId);

    // 기사: 본인 차량 운행 상태 변경
    DriverVehicleResponseDTO changeMyVehicleStatus(
            Long memberId, Long vehicleId, VehicleStatus status);
}