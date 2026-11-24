package com.transport.dto;

import java.time.LocalDateTime;

public record DriverSettlementItemResponseDTO(
        Long shipmentId,
        String cargoName,
        LocalDateTime completedAt,
        int fare
) {
}
