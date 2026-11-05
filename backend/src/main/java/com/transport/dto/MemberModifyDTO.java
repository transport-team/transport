package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 내 정보 수정 요청 (요구사항 COM-03: 연락처 · 비밀번호 중 연락처. 비밀번호는 PasswordChangeDTO)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberModifyDTO {

	@NotBlank(message = "연락처를 입력해 주세요.")
	@Pattern(regexp = "^01[016789]-\\d{3,4}-\\d{4}$", message = "연락처는 010-0000-0000 형식으로 입력해 주세요.")
	private String phone;
}
