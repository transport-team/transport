package com.transport.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// FAQ 작성 · 수정 요청 (관리자)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaqRequestDTO {

	@NotNull(message = "분류를 선택해 주세요.")
	@Pattern(regexp = "배송|결제|회원", message = "분류는 배송 · 결제 · 회원 중에서 골라 주세요.")
	private String category;

	@NotBlank(message = "질문을 입력해 주세요.")
	@Size(max = 255, message = "질문은 255자 이하로 입력해 주세요.")
	private String question;

	@NotBlank(message = "답변을 입력해 주세요.")
	@Size(max = 5000, message = "답변은 5,000자 이하로 입력해 주세요.")
	private String answer;

	@NotNull(message = "순서를 입력해 주세요.")
	@Min(value = 0, message = "순서는 0 이상으로 입력해 주세요.")
	private Integer sortOrder;  // 작을수록 위

	@NotNull(message = "노출 여부를 선택해 주세요.")
	private Boolean active;     // false = 숨기기
}
