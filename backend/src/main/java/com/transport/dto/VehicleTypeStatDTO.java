package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 차종별 결제 완료(PAID) 건수 · 매출 — 관리자 대시보드 차종별 차트 (배송의 요청 차종 기준)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleTypeStatDTO {

	private Long vehicleTypeId;
	private String vehicleTypeName;
	private long count;    // 결제 완료 건수
	private long revenue;  // 결제 금액 합계(원)
}
