package com.transport.service;

import com.transport.dto.SignupRequestDTO;

// 회원 (팀원 1)
public interface MemberService {

	// 회원가입 → 새 회원 id
	Long signup(SignupRequestDTO signupRequestDTO);
}
