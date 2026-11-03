package com.transport.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

// 에러 코드: 응답의 code는 상수 이름 (docs/api.md 에러 코드 표와 맞춘다)
// 자기 구역에만 추가하고 줄 끝은 항상 쉼표 (마지막 ;를 따로 둬서 여러 명이 동시에 추가해도 충돌이 적다)
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

	// 공통 · 인증 (팀원 1)
	INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 정보를 찾을 수 없습니다."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 요청 방식입니다."),
	FILE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "파일 크기가 너무 큽니다."),
	INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요."),
	DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일입니다."),
	TERMS_REQUIRED(HttpStatus.BAD_REQUEST, "필수 약관에 모두 동의해 주세요."),

	// 배차 · 정산 (팀원 2)

	// 배송 · 결제 (팀원 3)

	// 진행 · 실시간 · 리뷰 (팀원 4)

	;

	private final HttpStatus status;
	private final String message;
}
