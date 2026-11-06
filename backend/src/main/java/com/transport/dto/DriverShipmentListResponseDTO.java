package com.transport.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DriverShipmentListResponseDTO {

	private Long shipmentId;       // 배송 요청 ID

	private String originAddress;  // 출발지

	private String destAddress;    // 도착지

	private String cargoName;      // 화물 이름

	private int cargoWeightKg;     // 화물 무게

	private Long vehicleTypeId;    // 요청 차종 ID

	private int fare;              // 운임

	private LocalDateTime pickupAt; // 상차 예정일

}
