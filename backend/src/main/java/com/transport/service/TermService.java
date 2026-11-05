package com.transport.service;

import java.util.List;

import com.transport.domain.Term;
import com.transport.dto.TermDTO;

// 약관 (팀원 1)
public interface TermService {

	// 가입 화면에 보여 줄 약관 목록
	List<TermDTO> getActiveTerms();

	default TermDTO entityToDTO(Term term) {
		return TermDTO.builder()
				.id(term.getId())
				.title(term.getTitle())
				.content(term.getContent())
				.required(term.isRequired())
				.version(term.getVersion())
				.build();
	}
}
