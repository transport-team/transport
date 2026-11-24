package com.transport.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.TermDTO;
import com.transport.service.TermService;

import lombok.RequiredArgsConstructor;

// 약관 API: 가입 화면의 약관 목록
@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

	private final TermService termService;

	@GetMapping
	public List<TermDTO> list() {
		return termService.getActiveTerms();
	}
}
