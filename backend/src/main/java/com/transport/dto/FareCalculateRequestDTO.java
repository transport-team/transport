package com.transport.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareCalculateRequestDTO {

	@NotNull(message = "요청 차종 선택은 필수입니다.")
	private Long vehicleTypeId;

	@NotNull(message = "화물 유형 선택은 필수입니다.")
	private Long cargoTypeId;

	@NotBlank(message = "출발지 주소는 필수 입력 값입니다.")
	private String originAddress;

	@NotBlank(message = "도착지 주소는 필수 입력 값입니다.")
	private String destAddress;

	@NotNull(message = "화물 무게는 필수 입력 값입니다.")
	@Min(value = 1, message = "화물 무게는 최소 1kg 이상이어야 합니다.")
	private Integer cargoWeightKg;

	@NotNull(message = "희망 상차 일시는 필수 입력 값입니다.")
	private LocalDateTime pickupAt;
}
