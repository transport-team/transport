package com.transport.dto;

// 기사 정산 페이지와 같은 6개월 월별 추이를 위한 요약 데이터.
public record AdminSettlementTrendDTO(String month, long totalFare, long totalCommission) {
}
