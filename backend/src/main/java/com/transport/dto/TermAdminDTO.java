package com.transport.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 약관 (관리자 목록용) — 공개 TermDTO에 사용 여부 · 동의 수 · 등록일을 더한다
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermAdminDTO {

	private Long id;
	private String title;
	private String content;
	private boolean required;       // 필수 동의 여부
	private String version;
	private boolean active;         // false = 사용 중지 (가입 화면에서 빠짐)
	private long agreementCount;    // 이 버전에 동의한 회원 수 (0이면 삭제 가능)
	private LocalDateTime createdAt;
}
