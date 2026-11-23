package com.transport.dto;

import java.util.List;

// 기사 내 평점 · 리뷰 (DRV-07) — 평균은 소수 첫째 자리(리뷰가 없으면 0.0), 리뷰는 최신순 · 고객 이름은 가림(#125)
public record DriverReviewSummaryResponseDTO(
        double averageRating,
        int reviewCount,
        List<ReviewResponseDTO> reviews
) {
}
