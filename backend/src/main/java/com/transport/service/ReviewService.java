package com.transport.service;

import java.util.List;

import com.transport.dto.RatingStatDTO;
import com.transport.dto.ReviewResponseDTO;

// 리뷰 서비스 (8-1~8-2 — 팀원 4)
// getAverageRating은 docs/collaboration.md 약속 — 팀원 2가 배차 정렬에 호출한다
public interface ReviewService {

	/**
	 * 리뷰 작성 (배송완료 · 본인 배송 · 배송당 1회)
	 * @param shipmentId 배송 ID
	 * @param memberId 로그인 고객 회원 ID
	 * @param rating 별점 1~5
	 * @param content 후기 (500자 이하, null 허용)
	 * @return 저장된 리뷰
	 */
	ReviewResponseDTO createReview(Long shipmentId, Long memberId, int rating, String content);

	/**
	 * 특정 배송의 리뷰 (없으면 null — 상세 화면에서 폼/보기 분기용)
	 */
	ReviewResponseDTO getReviewByShipment(Long shipmentId);

	/**
	 * 기사 리뷰 목록 (최신순)
	 */
	List<ReviewResponseDTO> getDriverReviews(Long driverId);

	/**
	 * 기사 평균 평점 — 약속 메서드 (팀원 2 호출)
	 * 리뷰가 없으면 0.0 (null 대신 — 호출 쪽에서 정렬에 바로 쓰도록)
	 */
	double getAverageRating(Long driverId);

	/**
	 * 전체 리뷰 수 · 평균 평점 — 관리자 대시보드 "평균 평점" (읽기 전용 — 팀원 1 작성 · 팀원 4 리뷰)
	 * 평균은 getAverageRating처럼 소수 첫째 자리 반올림, 리뷰가 없으면 0건 · 0.0
	 */
	RatingStatDTO getRatingStat();
}