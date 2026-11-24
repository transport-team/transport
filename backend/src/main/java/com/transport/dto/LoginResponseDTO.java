package com.transport.dto;

import com.transport.domain.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 로그인 · 재발급 응답: 토큰 + 화면에서 쓸 회원 정보 (역할별 첫 화면 이동)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {

	private String accessToken;
	private String refreshToken;
	private Long memberId;
	private String email;
	private String name;
	private MemberRole role;
}
