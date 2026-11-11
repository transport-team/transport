package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.LocationLog;

import lombok.Builder;
import lombok.Getter;

// 위치 응답 (7-1 — GET .../locations/latest · SSE location 이벤트 본문과 같은 모양)
@Getter
@Builder
public class LocationResponseDTO {
	private double lat;
	private double lng;
	private LocalDateTime recordedAt;

	public static LocationResponseDTO from(LocationLog log) {
		return LocationResponseDTO.builder()
				.lat(log.getLat())
				.lng(log.getLng())
				.recordedAt(log.getRecordedAt())
				.build();
	}
}