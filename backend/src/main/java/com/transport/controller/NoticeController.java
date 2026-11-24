package com.transport.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.NoticeDTO;
import com.transport.dto.NoticeSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.NoticeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 공지사항 API: 목록 · 상세 (COM-05, 로그인 없이 — SecurityConfig 공개 목록)
@RestController
@RequestMapping("/api/notices")
@RequiredArgsConstructor
public class NoticeController {

	private final NoticeService noticeService;

	@GetMapping
	public PageResponseDTO<NoticeDTO> list(@Valid NoticeSearchDTO noticeSearchDTO) {
		return noticeService.getNotices(noticeSearchDTO);
	}

	@GetMapping("/{id}")
	public NoticeDTO read(@PathVariable Long id) {
		return noticeService.getNotice(id);
	}
}
