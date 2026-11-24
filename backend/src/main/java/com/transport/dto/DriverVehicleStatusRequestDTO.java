package com.transport.dto;

import com.transport.domain.enums.VehicleStatus;

// 기사 차량 상태 변경 요청 DTO
public record DriverVehicleStatusRequestDTO(VehicleStatus status) {
}