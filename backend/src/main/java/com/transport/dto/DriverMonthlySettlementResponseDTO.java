package com.transport.dto;

import java.time.LocalDateTime;

// 기사 월 실적 + 그 달 정산 상태
// settlementStatus: OPEN(관리자 월 마감 전) · PENDING(지급 대기) · PAID(지급 완료) · NONE(마감했지만 완료 실적이 없어 정산 없음)
// commissionRate · payoutAmount · paidAt: 정산 행이 있을 때만 (없으면 null), paidAt은 PAID일 때만
public record DriverMonthlySettlementResponseDTO(
        String settleMonth,
        long completedCount,
        long totalFare,
        String settlementStatus,
        Integer commissionRate,
        Integer payoutAmount,
        LocalDateTime paidAt
) {
}
