package com.transport.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.LoginRequestDTO;
import com.transport.dto.LoginResponseDTO;
import com.transport.dto.RefreshRequestDTO;
import com.transport.dto.SignupRequestDTO;
import com.transport.service.AuthService;
import com.transport.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 인증 API: 회원가입 · 로그인 · 토큰 재발급 (로그인 없이 부른다)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final MemberService memberService;
	private final AuthService authService;

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> signup(@Valid @RequestBody SignupRequestDTO signupRequestDTO) {
		return Map.of("memberId", memberService.signup(signupRequestDTO));
	}

	@PostMapping("/login")
	public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO loginRequestDTO) {
		return authService.login(loginRequestDTO);
	}

	// 액세스 토큰이 만료되면(TOKEN_EXPIRED) 프론트가 리프레시 토큰으로 호출
	@PostMapping("/refresh")
	public LoginResponseDTO refresh(@Valid @RequestBody RefreshRequestDTO refreshRequestDTO) {
		return authService.refresh(refreshRequestDTO.getRefreshToken());
	}
}
