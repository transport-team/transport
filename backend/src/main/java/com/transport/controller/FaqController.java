package com.transport.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.FaqDTO;
import com.transport.service.FaqService;

import lombok.RequiredArgsConstructor;

// FAQ API: 노출 중인 FAQ 목록 (COM-06, 로그인 없이 — SecurityConfig 공개 목록)
@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

	private final FaqService faqService;

	@GetMapping
	public List<FaqDTO> list(@RequestParam(required = false) String category) {
		return faqService.getFaqs(category);
	}
}
