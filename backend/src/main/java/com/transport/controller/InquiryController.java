package com.transport.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.InquiryDTO;
import com.transport.dto.InquiryRequestDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 1:1 문의 API (고객 · 기사): 등록 · 내 문의 (COM-07) — 관리자는 쓰지 않는다 (403)
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CUSTOMER', 'DRIVER')")
public class InquiryController {

	private final InquiryService inquiryService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> register(@AuthenticationPrincipal LoginMember loginMember,
			@Valid @RequestBody InquiryRequestDTO inquiryRequestDTO) {
		return Map.of("inquiryId", inquiryService.register(loginMember.getId(), inquiryRequestDTO));
	}

	@GetMapping("/my")
	public PageResponseDTO<InquiryDTO> my(@AuthenticationPrincipal LoginMember loginMember, @Valid PageRequestDTO pageRequestDTO) {
		return inquiryService.getMyInquiries(loginMember.getId(), pageRequestDTO);
	}
}
