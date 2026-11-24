package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 약관 (가입 화면용)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermDTO {

	private Long id;
	private String title;
	private String content;
	private boolean required;  // 필수 동의 여부
	private String version;
}
