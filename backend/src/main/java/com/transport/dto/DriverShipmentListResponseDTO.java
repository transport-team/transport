package com.transport.dto;

import java.time.LocalDateTime;
import java.util.List;

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

	// 예상 운송 소요 시간 (분)
	private int durationMin;

	// 배차 신청 가능 여부
	private boolean canApply;

	// 배차 신청 불가 사유 (차종 불일치, 휴무, 확정 배차 중복 등)
	private List<String> blockReasons;

	// 신청은 가능하지만 주의가 필요한 사항
	private List<String> warningMessages;

}
