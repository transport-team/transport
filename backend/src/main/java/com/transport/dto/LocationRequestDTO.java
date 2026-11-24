package com.transport.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 위치 전송 요청 (7-1 — POST /api/shipments/{id}/locations)
// 좌표 범위 밖 값(예: 위도 999)은 저장하지 않는다 — 지도 마커가 깨진다
@Getter
@NoArgsConstructor
public class LocationRequestDTO {

	@NotNull(message = "위도(lat)는 필수입니다.")
	@DecimalMin(value = "-90.0", message = "위도(lat)는 -90 ~ 90 사이여야 합니다.")
	@DecimalMax(value = "90.0", message = "위도(lat)는 -90 ~ 90 사이여야 합니다.")
	private Double lat;

	@NotNull(message = "경도(lng)는 필수입니다.")
	@DecimalMin(value = "-180.0", message = "경도(lng)는 -180 ~ 180 사이여야 합니다.")
	@DecimalMax(value = "180.0", message = "경도(lng)는 -180 ~ 180 사이여야 합니다.")
	private Double lng;
}