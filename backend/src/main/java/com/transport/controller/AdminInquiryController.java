package com.transport.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.InquiryAnswerDTO;
import com.transport.dto.InquiryDTO;
import com.transport.dto.InquirySearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 관리자 1:1 문의 API: 목록(상태 필터) · 답변 등록/수정(첫 답변이면 알림) · 삭제 (ADM-11)
@RestController
@RequestMapping("/api/admin/inquiries")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminInquiryController {

	private final InquiryService inquiryService;

	@GetMapping
	public PageResponseDTO<InquiryDTO> list(@Valid InquirySearchDTO inquirySearchDTO) {
		return inquiryService.getInquiries(inquirySearchDTO);
	}

	@PatchMapping("/{id}/answer")
	public InquiryDTO answer(@PathVariable Long id, @Valid @RequestBody InquiryAnswerDTO inquiryAnswerDTO) {
		return inquiryService.answer(id, inquiryAnswerDTO.getAnswer());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id) {
		inquiryService.remove(id);
	}
}
