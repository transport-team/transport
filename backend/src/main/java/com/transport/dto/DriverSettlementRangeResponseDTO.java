package com.transport.dto;

import java.time.LocalDate;

public record DriverSettlementRangeResponseDTO(
        LocalDate from,
        LocalDate to,
        long completedCount,
        long totalFare
) {
}
