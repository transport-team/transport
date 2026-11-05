package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 공지 작성 · 수정 요청 (관리자)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeRequestDTO {

	@NotBlank(message = "제목을 입력해 주세요.")
	@Size(max = 100, message = "제목은 100자 이하로 입력해 주세요.")
	private String title;

	@NotBlank(message = "내용을 입력해 주세요.")
	@Size(max = 5000, message = "내용은 5,000자 이하로 입력해 주세요.")
	private String content;
}
