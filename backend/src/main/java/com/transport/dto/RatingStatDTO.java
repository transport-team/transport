package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 전체 리뷰 수 · 평균 평점 — 관리자 대시보드 (ReviewService.getRatingStat)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RatingStatDTO {

	private long reviewCount;
	private double averageRating;  // 소수 첫째 자리 반올림, 리뷰가 없으면 0.0
}
