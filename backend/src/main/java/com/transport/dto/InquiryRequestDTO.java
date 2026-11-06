package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 1:1 문의 등록 요청 (고객 · 기사)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryRequestDTO {

	@NotBlank(message = "제목을 입력해 주세요.")
	@Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
	private String title;

	@NotBlank(message = "문의 내용을 입력해 주세요.")
	@Size(max = 5000, message = "문의 내용은 5,000자 이하로 입력해 주세요.")
	private String content;
}
