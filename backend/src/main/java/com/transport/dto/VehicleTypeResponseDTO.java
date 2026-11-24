
package com.transport.dto;

import lombok.Builder;
import lombok.Getter;

// 차종 목록 조회 응답 DTO
@Getter
@Builder
public class VehicleTypeResponseDTO {

	private Long id;
	private String code;
	private String name;
	private int maxLoadKg;
	private int baseFare;
	private int baseDistanceKm;
	private int perKmFare;
	private int nightRate;
	private int weekendRate;
	private int heavyRate;
	private boolean active;

}
