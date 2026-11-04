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

    private Long baseFare;         // 기본 운임
    private Long distanceFare;     // 거리 추가 운임
    private Long weightFare;       // 무게 추가 운임
    private Long surchargeFare;    // 시간대 할증 금액
    private Long totalFare;        // 최종 산정 운임

    private Double calculatedDistanceKm; // 계산에 사용된 거리(km)
}