package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// FAQ (공개 · 관리자 공통)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FaqDTO {

	private Long id;
	private String category;  // 배송 · 결제 · 회원
	private String question;
	private String answer;
	private int sortOrder;    // 작을수록 위
	private boolean active;   // 노출 여부
}
