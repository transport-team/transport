package com.transport.dto;

import com.transport.domain.enums.VehicleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRequestDTO {

	@Size(max = 20, message = "차량번호는 20자 이내로 입력해 주세요.")
	@NotBlank(message = "차량번호를 입력해 주세요.")
	private String plateNo;

	// 차종 ID 검증
	@NotNull(message = "차종을 선택해 주세요.")
	@Positive(message = "차종 정보가 올바르지 않습니다.")
	private Long vehicleTypeId;

	// 차량 상태 검증
	@NotNull(message = "차량 상태를 선택해 주세요.")
	private VehicleStatus status;

	// 담당 기사 ID 검증 (선택 입력)
	@Positive(message = "기사 정보가 올바르지 않습니다.")
	private Long driverId;

}
