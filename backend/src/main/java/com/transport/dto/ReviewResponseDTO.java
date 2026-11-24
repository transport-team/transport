package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.Review;
import com.transport.util.MaskingUtil;

import lombok.Builder;
import lombok.Getter;

// 리뷰 응답 (8-1~8-2)
@Getter
@Builder
public class ReviewResponseDTO {
	private Long id;
	private Long shipmentId;
	private Long driverId;
	private String customerName;  // 가려서 표시 (김*수 — 11-19 결정 4). 리뷰 응답은 모두 from()을 거친다
	private int rating;
	private String content;
	private LocalDateTime createdAt;

	public static ReviewResponseDTO from(Review r) {
		return ReviewResponseDTO.builder()
				.id(r.getId())
				.shipmentId(r.getShipment().getId())
				.driverId(r.getDriver().getId())
				.customerName(MaskingUtil.maskName(r.getCustomer().getName()))
				.rating(r.getRating())
				.content(r.getContent())
				.createdAt(r.getCreatedAt())
				.build();
	}
}