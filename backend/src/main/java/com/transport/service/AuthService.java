package com.transport.service;

import com.transport.dto.LoginRequestDTO;
import com.transport.dto.LoginResponseDTO;

// 로그인 · 토큰 재발급 (팀원 1)
public interface AuthService {

	LoginResponseDTO login(LoginRequestDTO loginRequestDTO);

	// 리프레시 토큰으로 액세스 · 리프레시 토큰을 둘 다 새로 발급
	LoginResponseDTO refresh(String refreshToken);
}
