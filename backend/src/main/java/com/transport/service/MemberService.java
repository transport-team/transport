package com.transport.service;

import com.transport.domain.Member;
import com.transport.dto.MemberDTO;
import com.transport.dto.MemberModifyDTO;
import com.transport.dto.PasswordChangeDTO;
import com.transport.dto.SignupRequestDTO;

// 회원 (팀원 1)
public interface MemberService {

	// 회원가입 → 새 회원 id
	Long signup(SignupRequestDTO signupRequestDTO);

	// 회원 정보 (내 정보 화면)
	MemberDTO getMember(Long memberId);

	// 내 정보 수정 (연락처) → 바뀐 내 정보
	MemberDTO modify(Long memberId, MemberModifyDTO memberModifyDTO);

	// 비밀번호 변경: 현재 비밀번호가 맞아야 하고, 지금과 같은 비밀번호로는 바꿀 수 없다
	void changePassword(Long memberId, PasswordChangeDTO passwordChangeDTO);

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
