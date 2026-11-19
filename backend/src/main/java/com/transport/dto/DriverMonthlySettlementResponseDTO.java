package com.transport.dto;

public record DriverMonthlySettlementResponseDTO(
        String settleMonth,
        long completedCount,
        long totalFare
) {
}
