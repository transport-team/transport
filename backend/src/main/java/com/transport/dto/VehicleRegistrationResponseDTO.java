package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.enums.VehicleRegistrationStatus;

import lombok.Builder;
import lombok.Getter;

// 차량 등록 신청 조회 응답 DTO
@Getter
@Builder
public class VehicleRegistrationResponseDTO {

    private Long id;

    // 신청한 기사 정보
    private Long driverId;
    private String driverName;

    // 신청 차량 정보
    private String plateNo;
    private Long vehicleTypeId;
    private String vehicleTypeName;

    // 신청 상태
    private VehicleRegistrationStatus status;

    // 거절 사유 및 처리 시각
    private String rejectionReason;
    private LocalDateTime reviewedAt;

    // 신청 시각
    private LocalDateTime createdAt;

}