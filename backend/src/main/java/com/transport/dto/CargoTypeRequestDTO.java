package com.transport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoTypeRequestDTO {

	@NotBlank(message = "화물 유형 이름은 필수입니다.")
	@Size(max = 30, message = "화물 유형 이름은 30자 이하여야 합니다.")
	private String name;

	@Min(value = 0, message = "추가요금은 0원 이상이어야 합니다.")
	private int extraFee;

	@Size(max = 255, message = "취급 안내는 255자 이하여야 합니다.")
	private String handlingNote;

	@Builder.Default
	private boolean active = true;
}