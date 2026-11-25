package com.transport.dto;

// 기사 한 명의 기간 안 운송완료 건수 · 운임 합계 (StatusHistoryService → 기사 실적, 팀원 4 → 2 약속)
public record DriverCompletedTotalDTO(long completedCount, long totalFare) {
}
