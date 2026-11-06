package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 기간 안 결제 완료(PAID) 건수 · 매출 — 관리자 대시보드 (PaymentService.getPaidStat)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaidStatDTO {

	private long count;    // 결제 완료 건수
	private long revenue;  // 결제 금액 합계(원)
}
