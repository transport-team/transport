package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.MemberDTO;
import com.transport.dto.MemberEnabledRequestDTO;
import com.transport.dto.MemberSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.MemberService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 관리자 회원 관리 API: 검색 · 비활성화 (ADM-10)
@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminMemberController {

	private final MemberService memberService;

	@GetMapping
	public PageResponseDTO<MemberDTO> list(@Valid MemberSearchDTO memberSearchDTO) {
		return memberService.getMembers(memberSearchDTO);
	}

	@PatchMapping("/{id}/enabled")
	public MemberDTO changeEnabled(@PathVariable Long id, @Valid @RequestBody MemberEnabledRequestDTO memberEnabledRequestDTO) {
		return memberService.changeEnabled(id, memberEnabledRequestDTO.getEnabled());
	}
}
