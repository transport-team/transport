package com.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.ReviewRequestDTO;
import com.transport.dto.ReviewResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.ReviewService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 리뷰 API (8-1~8-2 — 팀원 4)
@RestController
@RequiredArgsConstructor
@Log4j2
public class ReviewController {

	private final ReviewService reviewService;

	@Operation(summary = "리뷰 작성 (고객)", description = "배송완료 · 본인 배송 · 배송당 1회. driver는 배정 기사로 자동 설정")
	@PreAuthorize("hasRole('CUSTOMER')")
	@PostMapping("/api/shipments/{id}/review")
	public ResponseEntity<ReviewResponseDTO> createReview(
			@PathVariable Long id,
			@AuthenticationPrincipal LoginMember loginMember,
			@Valid @RequestBody ReviewRequestDTO requestDTO) {

		ReviewResponseDTO response = reviewService.createReview(
				id, loginMember.getId(), requestDTO.getRating(), requestDTO.getContent());
		return ResponseEntity.ok(response);
	}

	@Operation(summary = "배송의 리뷰 조회", description = "아직 없으면 204 (리뷰 폼/보기 분기용)")
	@GetMapping("/api/shipments/{id}/review")
	public ResponseEntity<ReviewResponseDTO> getReview(@PathVariable Long id) {
		ReviewResponseDTO response = reviewService.getReviewByShipment(id);
		return response == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(response);
	}

	@Operation(summary = "기사 리뷰 목록 + 평균 평점", description = "팀원 2 배차 정렬에서도 평균을 사용")
	@GetMapping("/api/drivers/{id}/reviews")
	public ResponseEntity<Map<String, Object>> getDriverReviews(@PathVariable Long id) {
		List<ReviewResponseDTO> reviews = reviewService.getDriverReviews(id);
		double average = reviewService.getAverageRating(id);
		return ResponseEntity.ok(Map.of(
				"reviews", reviews,
				"averageRating", average));
	}
}