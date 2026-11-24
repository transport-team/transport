package com.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.FaqDTO;
import com.transport.dto.FaqRequestDTO;
import com.transport.service.FaqService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 관리자 FAQ 관리 API: 전체 목록(숨김 포함) · 작성 · 수정 · 삭제 (ADM-11)
@RestController
@RequestMapping("/api/admin/faqs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFaqController {

	private final FaqService faqService;

	@GetMapping
	public List<FaqDTO> list() {
		return faqService.getAllFaqs();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> register(@Valid @RequestBody FaqRequestDTO faqRequestDTO) {
		return Map.of("faqId", faqService.register(faqRequestDTO));
	}

	@PutMapping("/{id}")
	public FaqDTO modify(@PathVariable Long id, @Valid @RequestBody FaqRequestDTO faqRequestDTO) {
		return faqService.modify(id, faqRequestDTO);
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id) {
		faqService.remove(id);
	}
}
