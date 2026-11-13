package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 기사별 배송완료(COMPLETED) 건수 — 관리자 대시보드 기사 순위 (StatusHistoryService.getCompletedCountsByDriver)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverCompletedStatDTO {

	private Long driverId;        // driver.id (회원 ID 아님 — ReviewService.getAverageRating에 그대로 넘긴다)
	private String driverName;    // 기사 회원 이름
	private long completedCount;  // 기간 안 배송완료 건수
}
