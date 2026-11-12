package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.Review;

import lombok.Builder;
import lombok.Getter;

// 리뷰 응답 (8-1~8-2)
@Getter
@Builder
public class ReviewResponseDTO {
	private Long id;
	private Long shipmentId;
	private Long driverId;
	private String customerName;  // 마스킹 없이 표시 (팀 합의 필요하면 변경)
	private int rating;
	private String content;
	private LocalDateTime createdAt;

	public static ReviewResponseDTO from(Review r) {
		return ReviewResponseDTO.builder()
				.id(r.getId())
				.shipmentId(r.getShipment().getId())
				.driverId(r.getDriver().getId())
				.customerName(r.getCustomer().getName())
				.rating(r.getRating())
				.content(r.getContent())
				.createdAt(r.getCreatedAt())
				.build();
	}
}