package com.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.TermActiveRequestDTO;
import com.transport.dto.TermAdminDTO;
import com.transport.dto.TermRequestDTO;
import com.transport.service.TermService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 관리자 약관 관리 API: 전체 목록(동의 수) · 등록 · 개정 · 사용 중지/다시 사용 · 삭제 (ADM-11)
// 약관 내용은 고치지 않는다 (PUT 없음) — 바꾸려면 새 버전으로 개정
@RestController
@RequestMapping("/api/admin/terms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTermController {

	private final TermService termService;

	@GetMapping
	public List<TermAdminDTO> list() {
		return termService.getAllTerms();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> register(@Valid @RequestBody TermRequestDTO termRequestDTO) {
		return Map.of("termId", termService.register(termRequestDTO));
	}

	@PostMapping("/{id}/revisions")
	@ResponseStatus(HttpStatus.CREATED)
	public Map<String, Long> revise(@PathVariable Long id, @Valid @RequestBody TermRequestDTO termRequestDTO) {
		return Map.of("termId", termService.revise(id, termRequestDTO));
	}

	@PatchMapping("/{id}/active")
	public TermAdminDTO changeActive(@PathVariable Long id, @Valid @RequestBody TermActiveRequestDTO termActiveRequestDTO) {
		return termService.changeActive(id, termActiveRequestDTO.getActive());
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void remove(@PathVariable Long id) {
		termService.remove(id);
	}
}
