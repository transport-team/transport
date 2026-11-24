package com.transport.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 하루 결제 완료(PAID) 건수 · 매출 — 관리자 대시보드 일별 차트 (GET /api/admin/stats/daily)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DailyStatDTO {

	private LocalDate date;  // 결제일 (paidAt의 날짜)
	private long count;      // 결제 완료 건수
	private long revenue;    // 결제 금액 합계(원)
}
