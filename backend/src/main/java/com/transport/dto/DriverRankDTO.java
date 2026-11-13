package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 관리자 대시보드 기사 순위 한 줄 — 이번 달 배송완료 건수 많은 순 (세는 기준은 docs/decisions.md)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverRankDTO {

	private int rank;              // 1부터. 완료 건수가 같으면 같은 순위 (7건 · 7건 · 3건 → 1 · 1 · 3)
	private Long driverId;         // driver.id
	private String driverName;
	private long completedCount;   // 이번 달 배송완료 건수
	private double averageRating;  // 전체 기간 평균 평점 (ReviewService.getAverageRating, 리뷰가 없으면 0.0)
}
