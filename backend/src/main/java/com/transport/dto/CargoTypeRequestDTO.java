package com.transport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 화물 종류 등록 · 수정 입력
// extraFee · active는 빼거나 null로 보내도 된다 — 추가요금 0원 · 사용함 (전에는 null이면 400 "입력값이 올바르지 않습니다", #108 댓글)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CargoTypeRequestDTO {

	@NotBlank(message = "화물 유형 이름은 필수입니다.")
	@Size(max = 30, message = "화물 유형 이름은 30자 이하여야 합니다.")
	private String name;

	@Min(value = 0, message = "추가요금은 0원 이상이어야 합니다.")
	@Getter(AccessLevel.NONE)
	private Integer extraFee;

	@Size(max = 255, message = "취급 안내는 255자 이하여야 합니다.")
	private String handlingNote;

	@Getter(AccessLevel.NONE)
	private Boolean active;

	// 빠지면 0원
	public int getExtraFee() {
		return extraFee == null ? 0 : extraFee;
	}

	// 빠지면 사용함
	public boolean isActive() {
		return active == null || active;
	}
}
