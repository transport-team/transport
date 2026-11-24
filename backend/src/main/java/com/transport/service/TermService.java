package com.transport.service;

import java.util.List;

import com.transport.domain.Term;
import com.transport.dto.TermAdminDTO;
import com.transport.dto.TermDTO;
import com.transport.dto.TermRequestDTO;

// 약관 (팀원 1) — 약관은 동의 이력의 근거라 내용을 고치지 않는다. 바꾸려면 새 버전으로 개정 (docs/decisions.md)
public interface TermService {

	// 가입 화면에 보여 줄 약관 목록 (필수 먼저, 그다음 등록 순)
	List<TermDTO> getActiveTerms();

	// 관리자: 지난 버전 · 사용 중지까지 전체 + 약관별 동의 수
	List<TermAdminDTO> getAllTerms();

	// 관리자: 새 약관 (바로 사용 중) → 새 약관 id
	Long register(TermRequestDTO termRequestDTO);

	// 관리자: 새 버전으로 개정 — 새 버전은 사용 중, 이전 버전은 사용 중지 (동의 이력은 이전 버전에 남는다) → 새 버전 id
	Long revise(Long id, TermRequestDTO termRequestDTO);

	// 관리자: 사용 중지 · 다시 사용
	TermAdminDTO changeActive(Long id, boolean active);

	// 관리자: 삭제 — 동의한 회원이 없을 때만 (잘못 만든 약관 정리용)
	void remove(Long id);

	default TermDTO entityToDTO(Term term) {
		return TermDTO.builder()
				.id(term.getId())
				.title(term.getTitle())
				.content(term.getContent())
				.required(term.isRequired())
				.version(term.getVersion())
				.build();
	}

	default TermAdminDTO entityToAdminDTO(Term term, long agreementCount) {
		return TermAdminDTO.builder()
				.id(term.getId())
				.title(term.getTitle())
				.content(term.getContent())
				.required(term.isRequired())
				.version(term.getVersion())
				.active(term.isActive())
				.agreementCount(agreementCount)
				.createdAt(term.getCreatedAt())
				.build();
	}
}
