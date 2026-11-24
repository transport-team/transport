package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 토큰 재발급 요청
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RefreshRequestDTO {

	@NotBlank(message = "리프레시 토큰이 없습니다.")
	private String refreshToken;
}
