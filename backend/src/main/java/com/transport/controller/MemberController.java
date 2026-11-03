package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.MemberDTO;
import com.transport.security.LoginMember;
import com.transport.service.MemberService;

import lombok.RequiredArgsConstructor;

// 회원 API: 내 정보 (로그인 사용자 꺼내는 방법의 예시)
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	@GetMapping("/me")
	@PreAuthorize("isAuthenticated()")  // 권한 전환일 전이라도 이 API는 로그인이 필요하다
	public MemberDTO me(@AuthenticationPrincipal LoginMember loginMember) {
		return memberService.getMember(loginMember.getId());
	}
}
