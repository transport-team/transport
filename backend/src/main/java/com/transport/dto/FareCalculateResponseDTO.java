package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareCalculateResponseDTO {

	// 운임 내역 (fare.md 계산식 기준, 금액은 int)
	private int baseFare;       // 기본 운임
	private int distanceFare;   // 기본 거리 초과 운임
	private int cargoFare;      // 화물 종류 추가 요금
	private int surchargeFare;  // 할증 금액 (야간 · 주말 · 고중량)
	private int totalFare;      // 최종 운임 (100원 미만 버림)

	// 서버가 계산한 거리 · 좌표
	private double distanceKm;
	private Integer durationMin; // 길찾기 실패 시 null
	private double originLat;
	private double originLng;
	private double destLat;
	private double destLng;

	// 무게 기준 차종 추천
	private Long recommendedVehicleTypeId; // 5톤 초과면 null
	private boolean inquiryRequired;       // true면 "문의 안내"
}
