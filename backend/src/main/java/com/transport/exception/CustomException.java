package com.transport.exception;

import lombok.Getter;

// 규칙 위반을 알릴 때 서비스에서 던진다: throw new CustomException(ErrorCode.NOT_FOUND)
@Getter
public class CustomException extends RuntimeException {

	private final ErrorCode errorCode;

	public CustomException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}

	// 상황마다 문구가 다를 때 (예: "배차완료 상태에서만 상차 처리할 수 있습니다.")
	public CustomException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}
}
