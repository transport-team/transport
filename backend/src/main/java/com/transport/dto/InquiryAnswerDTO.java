package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 1:1 문의 답변 등록 · 수정 요청 (관리자)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryAnswerDTO {

	@NotBlank(message = "답변을 입력해 주세요.")
	@Size(max = 5000, message = "답변은 5,000자 이하로 입력해 주세요.")
	private String answer;
}
