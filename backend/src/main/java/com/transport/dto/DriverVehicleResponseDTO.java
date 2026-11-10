package com.transport.dto;

import com.transport.domain.enums.VehicleStatus;

import lombok.Builder;
import lombok.Getter;

// 기사 본인 차량 목록 응답 DTO
@Getter
@Builder
public class DriverVehicleResponseDTO {

    private Long vehicleId;

    private String plateNo;

    private Long vehicleTypeId;

    private VehicleStatus status;
}
