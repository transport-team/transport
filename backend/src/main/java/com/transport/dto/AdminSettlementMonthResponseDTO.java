package com.transport.dto;

import java.time.LocalDateTime;
import java.util.List;

public record AdminSettlementMonthResponseDTO(
        String month,
        int standardCommissionRate,
        boolean closed,
        LocalDateTime closedAt,
        long totalFare,
        long totalCommission,
        long totalPayout,
        long pendingCount,
        long paidCount,
        long uncreatedCount,
        long missingRateCount,
        List<Item> items) {

    public record Item(
            Long id,
            Long driverId,
            String driverName,
            String driverEmail,
            long completedCount,
            long totalFare,
            Integer commissionRate,
            Integer payoutAmount,
            String status,
            LocalDateTime paidAt,
            boolean changedSinceDraft,
            String paidByAdminEmail,
            String transferReference) {
    }
}
