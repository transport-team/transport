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
	private String customerName;
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

	// 고객 이름 마스킹: "김*수", "박*", "이*민" 형태
	public static String maskName(String name) {
		if (name == null || name.isBlank()) return "";
		int len = name.length();
		if (len == 1) return name;
		if (len == 2) return name.charAt(0) + "*";
		return name.charAt(0) + "*" + name.charAt(len - 1);
	}

	// 마스킹된 복사본 반환 (리뷰 목록 조회용)
	public ReviewResponseDTO withMaskedName() {
		return ReviewResponseDTO.builder()
				.id(this.id)
				.shipmentId(this.shipmentId)
				.driverId(this.driverId)
				.customerName(maskName(this.customerName))
				.rating(this.rating)
				.content(this.content)
				.createdAt(this.createdAt)
				.build();
	}
}
