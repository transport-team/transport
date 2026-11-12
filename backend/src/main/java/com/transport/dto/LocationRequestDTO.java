package com.transport.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 위치 전송 요청 (7-1 — POST /api/shipments/{id}/locations)
@Getter
@NoArgsConstructor
public class LocationRequestDTO {

	@NotNull(message = "위도(lat)는 필수입니다.")
	private Double lat;

	@NotNull(message = "경도(lng)는 필수입니다.")
	private Double lng;
}