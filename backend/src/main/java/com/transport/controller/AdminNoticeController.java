package com.transport.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.NoticeDTO;
import com.transport.dto.NoticeRequestDTO;
import com.transport.security.LoginMember;
import com.transport.service.NoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 관리자 공지 관리 API: 작성 · 수정 · 삭제 (ADM-11, 목록 · 상세는 공개 API를 같이 쓴다)
@RestController
@RequestMapping("/api/admin/notices")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminNoticeController {

	private final NoticeService noticeService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> register(@AuthenticationPrincipal LoginMember loginMember, @Valid @RequestBody NoticeRequestDTO noticeRequestDTO) {
		return Map.of("noticeId", noticeService.register(loginMember.getId(), noticeRequestDTO));
	}

	@PutMapping("/{id}")
	public NoticeDTO modify(@PathVariable Long id, @Valid @RequestBody NoticeRequestDTO noticeRequestDTO) {
		return noticeService.modify(id, noticeRequestDTO);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id) {
		noticeService.remove(id);
	}
}
