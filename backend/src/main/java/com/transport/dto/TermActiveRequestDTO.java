package com.transport.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 약관 사용 중지 · 다시 사용 요청 (관리자)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermActiveRequestDTO {

	@NotNull(message = "변경할 상태를 선택해 주세요.")
	private Boolean active;  // false = 사용 중지 (가입 화면에서 빠짐)
}
