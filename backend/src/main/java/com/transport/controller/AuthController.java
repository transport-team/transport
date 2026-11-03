package com.transport.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.SignupRequestDTO;
import com.transport.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 인증 API: 회원가입 (로그인 · 토큰 재발급은 J단계 PR 2)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final MemberService memberService;

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> signup(@Valid @RequestBody SignupRequestDTO signupRequestDTO) {
		return Map.of("memberId", memberService.signup(signupRequestDTO));
	}
}
