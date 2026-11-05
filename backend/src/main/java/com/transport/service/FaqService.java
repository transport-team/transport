package com.transport.service;

import java.util.List;

import com.transport.domain.Faq;
import com.transport.dto.FaqDTO;
import com.transport.dto.FaqRequestDTO;

// FAQ (팀원 1)
public interface FaqService {

	// 공개 FAQ: 노출 중인 것만 순서대로 (category가 없으면 전체 분류)
	List<FaqDTO> getFaqs(String category);

	// 관리자: 숨긴 것까지 전체
	List<FaqDTO> getAllFaqs();

	// 관리자: FAQ 작성 → 새 FAQ id
	Long register(FaqRequestDTO faqRequestDTO);

	// 관리자: FAQ 수정 (노출 켜기 · 끄기 포함) → 바뀐 FAQ
	FaqDTO modify(Long faqId, FaqRequestDTO faqRequestDTO);

	// 관리자: FAQ 삭제
	void remove(Long faqId);

	default FaqDTO entityToDTO(Faq faq) {
		return FaqDTO.builder()
				.id(faq.getId())
				.category(faq.getCategory())
				.question(faq.getQuestion())
				.answer(faq.getAnswer())
				.sortOrder(faq.getSortOrder())
				.active(faq.isActive())
				.build();
	}
}
