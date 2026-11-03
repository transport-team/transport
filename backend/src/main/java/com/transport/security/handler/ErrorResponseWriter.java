package com.transport.security.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.MediaType;

import com.transport.dto.ErrorResponseDTO;
import com.transport.exception.ErrorCode;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

// 보안 필터 단계의 에러 응답 (컨트롤러 밖이라 GlobalExceptionHandler가 잡지 못한다) — 형식은 같은 {code, message}
@RequiredArgsConstructor
public class ErrorResponseWriter {

	private final JsonMapper jsonMapper;

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		jsonMapper.writeValue(response.getWriter(), new ErrorResponseDTO(errorCode.name(), errorCode.getMessage()));
	}
}
