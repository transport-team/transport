package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 약관 등록 · 개정 요청 (관리자) — 개정은 이전 버전을 고치지 않고 이 내용으로 새 버전을 만든다
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermRequestDTO {

	@NotBlank(message = "약관 이름을 입력해 주세요.")
	@Size(max = 100, message = "약관 이름은 100자 이하로 입력해 주세요.")
	private String title;

	@NotBlank(message = "약관 내용을 입력해 주세요.")
	@Size(max = 15000, message = "약관 내용은 15,000자 이하로 입력해 주세요.")  // TEXT(65,535바이트)에 4바이트 글자로도 들어가는 길이
	private String content;

	@NotNull(message = "필수 여부를 선택해 주세요.")
	private Boolean required;  // true = 가입할 때 꼭 동의

	@NotBlank(message = "버전을 입력해 주세요.")
	@Size(max = 20, message = "버전은 20자 이하로 입력해 주세요.")
	private String version;    // 예: v1.1
}
