package com.transport.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 거리 · 소요시간 · 운임은 받지 않는다. 신청할 때 서버가 계산해서 payment에 저장한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentRequestDTO {

	// 차종 및 화물 종류 PK (Shipment 엔티티 연관관계 매핑용)
	@NotNull(message = "요청 차종 선택은 필수입니다.")
	private Long vehicleTypeId;

	@NotNull(message = "화물 유형 선택은 필수입니다.")
	private Long cargoTypeId;

	// 출발지 정보
	@NotBlank(message = "출발지 주소는 필수입니다.")
	private String originAddress;

	private String originDetail;

	// 도착지 정보
	@NotBlank(message = "도착지 주소는 필수입니다.")
	private String destAddress;

	private String destDetail;

	// 화물 상세 정보
	@NotBlank(message = "화물명은 필수입니다.")
	private String cargoName;

	@NotNull(message = "화물 무게는 필수입니다.")
	@Min(value = 1, message = "화물 무게는 1kg 이상이어야 합니다.")
	private Integer cargoWeightKg;

	@NotNull(message = "희망 상차 일시는 필수입니다.")
	private LocalDateTime pickupAt;

	// 받는 사람 정보
	@NotBlank(message = "받는 사람 이름은 필수입니다.")
	private String receiverName;

	@NotBlank(message = "받는 사람 연락처는 필수입니다.")
	@Pattern(regexp = "^01(?:0|1|[6-9])-(?:\\d{3}|\\d{4})-\\d{4}$", message = "연락처는 010-0000-0000 형식으로 입력해 주세요.")
	private String receiverPhone;

	@Size(max = 500, message = "요청사항은 500자 이하로 작성해주세요.")
	private String memo; // 요청사항
}
