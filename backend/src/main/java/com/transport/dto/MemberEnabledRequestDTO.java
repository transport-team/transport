package com.transport.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 회원 비활성화 · 다시 활성화 요청 (관리자)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberEnabledRequestDTO {

	@NotNull(message = "변경할 상태를 선택해 주세요.")
	private Boolean enabled;  // false = 비활성화 (로그인 불가)
}
