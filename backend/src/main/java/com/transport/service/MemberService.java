package com.transport.service;

import com.transport.domain.Member;
import com.transport.dto.MemberDTO;
import com.transport.dto.SignupRequestDTO;

// 회원 (팀원 1)
public interface MemberService {

	// 회원가입 → 새 회원 id
	Long signup(SignupRequestDTO signupRequestDTO);

	// 회원 정보 (내 정보 화면)
	MemberDTO getMember(Long memberId);

	default MemberDTO entityToDTO(Member member) {
		return MemberDTO.builder()
				.id(member.getId())
				.email(member.getEmail())
				.name(member.getName())
				.phone(member.getPhone())
				.role(member.getRole())
				.build();
	}
}
