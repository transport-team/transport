package com.transport.dto;

import java.time.LocalDateTime;
import com.transport.domain.enums.VehicleStatus;

// 관리자 기사 관리에서 볼 차량의 최소 정보
public record AdminDriverVehicleResponseDTO(
        Long id,
        String plateNo,
        String vehicleTypeName,
        VehicleStatus status,
        LocalDateTime createdAt
) {}
