package com.transport.controller.advice;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.transport.dto.ErrorResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;

// 모든 에러 응답을 { code, message }로 맞춘다
// Spring 기본 예외(잘못된 JSON, 없는 URL, 지원하지 않는 메서드 등)는 부모 클래스가 상태 코드를 정하고, 본문만 여기서 바꾼다
@Log4j2
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ResponseEntity<ErrorResponseDTO> handleCustomException(CustomException e) {
		ErrorCode errorCode = e.getErrorCode();
		return ResponseEntity.status(errorCode.getStatus())
				.body(new ErrorResponseDTO(errorCode.name(), e.getMessage()));
	}

	// @PreAuthorize 권한 예외는 Spring Security가 401 · 403으로 처리하도록 그대로 던진다
	@ExceptionHandler({AccessDeniedException.class, AuthenticationException.class})
	public void rethrowSecurityException(RuntimeException e) {
		throw e;
	}

	// 예상하지 못한 예외: 원인은 서버 로그에만 남기고 응답은 기본 메시지
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleUnexpectedException(Exception e) {
		log.error("처리하지 못한 예외", e);
		ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
		return ResponseEntity.status(errorCode.getStatus())
				.body(new ErrorResponseDTO(errorCode.name(), errorCode.getMessage()));
	}

	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());

		ErrorCode errorCode = toErrorCode(statusCode);
		String message = errorCode.getMessage();
		if (ex instanceof MethodArgumentNotValidException e && e.getFieldError() != null) {
			message = e.getFieldError().getDefaultMessage();  // @Valid 실패: DTO에 적은 message
		}
		return super.handleExceptionInternal(ex, new ErrorResponseDTO(errorCode.name(), message), headers, statusCode, request);
	}

	private ErrorCode toErrorCode(HttpStatusCode statusCode) {
		return switch (statusCode.value()) {
			case 404 -> ErrorCode.NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 413 -> ErrorCode.FILE_TOO_LARGE;
			default -> statusCode.is4xxClientError() ? ErrorCode.INVALID_INPUT : ErrorCode.INTERNAL_SERVER_ERROR;
		};
	}
}
