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
	LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "로그인 시간이 만료되었습니다."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "로그인 정보가 올바르지 않습니다. 다시 로그인해 주세요."),
	ACCOUNT_DISABLED(HttpStatus.FORBIDDEN, "이용이 중지된 계정입니다. 관리자에게 문의해 주세요."),
	DRIVER_NOT_APPROVED(HttpStatus.FORBIDDEN, "관리자 승인 후 로그인할 수 있습니다."),
	WRONG_PASSWORD(HttpStatus.BAD_REQUEST, "현재 비밀번호가 맞지 않습니다."),

	// 배차 · 정산 (팀원 2)
	DUPLICATE_VEHICLE_PLATE(HttpStatus.CONFLICT, "이미 등록된 차량번호입니다."),
	DUPLICATE_DRIVER_VEHICLE_TYPE(HttpStatus.CONFLICT, "차종당 1대만 등록 가능합니다."),

	// 배송 · 결제 (팀원 3)
	DISTANCE_CALCULATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "거리 계산 중 오류가 발생했습니다."),
	UNSUPPORTED_CARGO_WEIGHT(HttpStatus.BAD_REQUEST, "지원하지 않는 화물 무게이거나 별도 문의가 필요합니다."),
	PAYMENT_AMOUNT_MISMATCH(HttpStatus.BAD_REQUEST, "결제 요청 금액이 서버 산출 금액과 일치하지 않습니다."),
	PAYMENT_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "현재 상태에서는 결제를 승인할 수 없습니다."),
	PAYMENT_ALREADY_COMPLETED(HttpStatus.CONFLICT, "이미 승인된 결제입니다."),
	PAYMENT_FAILED(HttpStatus.BAD_GATEWAY, "결제 승인에 실패했습니다."),
	SHIPMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 배송 건을 찾을 수 없습니다."),

	// 진행 · 실시간 · 리뷰 (팀원 4)
	INVALID_STATUS_CHANGE(HttpStatus.BAD_REQUEST, "허용되지 않는 상태 변경입니다."),
	NOT_ASSIGNED_DRIVER(HttpStatus.FORBIDDEN, "배정된 기사가 아닙니다."),
	PHOTO_REQUIRED(HttpStatus.BAD_REQUEST, "필수 사진이 없습니다."),
	NOT_IN_TRANSIT(HttpStatus.BAD_REQUEST, "운송중 상태가 아닙니다."),
	REVIEW_EXISTS(HttpStatus.CONFLICT, "이미 리뷰를 작성했습니다."),
	REVIEW_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "배송완료 건만 리뷰 가능합니다."),

	;

	private final HttpStatus status;
	private final String message;
}