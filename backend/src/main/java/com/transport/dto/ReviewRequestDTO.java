package com.transport.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 리뷰 작성 요청 (8-1 — POST /api/shipments/{id}/review)
@Getter
@NoArgsConstructor
public class ReviewRequestDTO {

	@NotNull(message = "별점을 선택해 주세요.")
	@Min(value = 1, message = "별점은 1점 이상이어야 합니다.")
	@Max(value = 5, message = "별점은 5점 이하여야 합니다.")
	private Integer rating;

	@Size(max = 500, message = "후기는 500자 이하로 작성해 주세요.")
	private String content;
}