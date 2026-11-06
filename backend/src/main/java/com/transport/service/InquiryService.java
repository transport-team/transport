package com.transport.service;

import com.transport.domain.Inquiry;
import com.transport.domain.Member;
import com.transport.dto.InquiryDTO;
import com.transport.dto.InquiryRequestDTO;
import com.transport.dto.InquirySearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;

// 1:1 문의 (팀원 1) — 고객 · 기사가 쓰고, 관리자가 답한다 (COM-07 · ADM-11)
public interface InquiryService {

	// 문의 등록 (고객 · 기사) → 문의 id
	Long register(Long memberId, InquiryRequestDTO inquiryRequestDTO);

	// 내 문의 (최신순, 답변 포함)
	PageResponseDTO<InquiryDTO> getMyInquiries(Long memberId, PageRequestDTO pageRequestDTO);

	// 관리자: 문의 목록 (상태 필터 · 최신순)
	PageResponseDTO<InquiryDTO> getInquiries(InquirySearchDTO inquirySearchDTO);

	// 관리자: 답변 등록 · 수정 — 첫 답변이면 작성자에게 알림 (NotificationService, 팀원 4)
	InquiryDTO answer(Long id, String answer);

	// 관리자: 삭제 (스팸 정리)
	void remove(Long id);

	default InquiryDTO entityToDTO(Inquiry inquiry) {
		Member member = inquiry.getMember();
		return InquiryDTO.builder()
				.id(inquiry.getId())
				.title(inquiry.getTitle())
				.content(inquiry.getContent())
				.answer(inquiry.getAnswer())
				.answeredAt(inquiry.getAnsweredAt())
				.status(inquiry.getStatus())
				.createdAt(inquiry.getCreatedAt())
				.memberId(member.getId())
				.memberName(member.getName())
				.memberEmail(member.getEmail())
				.memberRole(member.getRole())
				.build();
	}
}
