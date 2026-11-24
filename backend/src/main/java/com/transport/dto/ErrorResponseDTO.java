package com.transport.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 에러 응답 본문: { "code": "NOT_FOUND", "message": "요청한 정보를 찾을 수 없습니다." }
@Getter
@AllArgsConstructor
public class ErrorResponseDTO {

	private final String code;
	private final String message;
}
