package com.transport.service;

import java.util.Optional;

import com.transport.domain.Member;
import com.transport.dto.MemberDTO;
import com.transport.dto.MemberModifyDTO;
import com.transport.dto.MemberSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.PasswordChangeDTO;
import com.transport.dto.SignupRequestDTO;

// 회원 (팀원 1)
public interface MemberService {

	// 회원가입 → 새 회원 id
	Long signup(SignupRequestDTO signupRequestDTO);

	// 회원 정보 (내 정보 화면)
	MemberDTO getMember(Long memberId);

	// 다른 도메인용 회원 엔티티 (예: 배송 신청 고객 연결) — 없으면 빈 값. 다른 도메인은 MemberRepository 대신 이 메서드를 쓴다
	Optional<Member> findById(Long memberId);

	// 내 정보 수정 (연락처) → 바뀐 내 정보
	MemberDTO modify(Long memberId, MemberModifyDTO memberModifyDTO);

	// 비밀번호 변경: 현재 비밀번호가 맞아야 하고, 지금과 같은 비밀번호로는 바꿀 수 없다
	void changePassword(Long memberId, PasswordChangeDTO passwordChangeDTO);

	// 관리자 회원 목록: 역할 · 상태 · 이름/이메일 검색, 최신 가입 순
	PageResponseDTO<MemberDTO> getMembers(MemberSearchDTO memberSearchDTO);

	// 관리자: 비활성화(false) · 다시 활성화(true) → 바뀐 회원 정보. 관리자 계정은 바꿀 수 없다
	MemberDTO changeEnabled(Long memberId, boolean enabled);

	default MemberDTO entityToDTO(Member member) {
		return MemberDTO.builder()
				.id(member.getId())
				.email(member.getEmail())
				.name(member.getName())
				.phone(member.getPhone())
				.role(member.getRole())
				.enabled(member.isEnabled())
				.createdAt(member.getCreatedAt())
				.build();
	}
}
