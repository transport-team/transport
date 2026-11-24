package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 비밀번호 변경 요청: 현재 비밀번호를 확인한 뒤 바꾼다
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PasswordChangeDTO {

	@NotBlank(message = "현재 비밀번호를 입력해 주세요.")
	private String currentPassword;

	@NotBlank(message = "새 비밀번호를 입력해 주세요.")
	@Size(min = 8, max = 64, message = "새 비밀번호는 8~64자로 입력해 주세요.")
	private String newPassword;
}
