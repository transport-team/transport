package com.transport.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.MemberDTO;
import com.transport.dto.MemberModifyDTO;
import com.transport.dto.PasswordChangeDTO;
import com.transport.security.LoginMember;
import com.transport.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 회원 API: 내 정보 조회 · 수정 · 비밀번호 변경 (로그인 사용자 꺼내는 방법의 예시)
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")  // 모두 로그인 필요
public class MemberController {

	private final MemberService memberService;

	@GetMapping("/me")
	public MemberDTO me(@AuthenticationPrincipal LoginMember loginMember) {
		return memberService.getMember(loginMember.getId());
	}

	@PutMapping("/me")
	public MemberDTO modify(@AuthenticationPrincipal LoginMember loginMember, @Valid @RequestBody MemberModifyDTO memberModifyDTO) {
		return memberService.modify(loginMember.getId(), memberModifyDTO);
	}

	@PatchMapping("/me/password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void changePassword(@AuthenticationPrincipal LoginMember loginMember, @Valid @RequestBody PasswordChangeDTO passwordChangeDTO) {
		memberService.changePassword(loginMember.getId(), passwordChangeDTO);
	}
}
