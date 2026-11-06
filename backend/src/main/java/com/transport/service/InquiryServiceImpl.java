package com.transport.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Inquiry;
import com.transport.domain.Member;
import com.transport.domain.enums.InquiryStatus;
import com.transport.domain.enums.NotificationType;
import com.transport.dto.InquiryDTO;
import com.transport.dto.InquiryRequestDTO;
import com.transport.dto.InquirySearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.InquiryRepository;
import com.transport.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

// InquiryService 구현
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService {

	private static final Sort LATEST = Sort.by("id").descending();

	private final InquiryRepository inquiryRepository;
	private final MemberRepository memberRepository;
	private final NotificationService notificationService;  // 팀원 4 약속 메서드 (docs/collaboration.md)

	@Override
	@Transactional
	public Long register(Long memberId, InquiryRequestDTO inquiryRequestDTO) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "회원을 찾을 수 없습니다."));
		Inquiry inquiry = Inquiry.builder()
				.member(member)
				.title(inquiryRequestDTO.getTitle().strip())
				.content(inquiryRequestDTO.getContent())
				.status(InquiryStatus.WAITING)
				.build();
		return inquiryRepository.save(inquiry).getId();
	}

	@Override
	public PageResponseDTO<InquiryDTO> getMyInquiries(Long memberId, PageRequestDTO pageRequestDTO) {
		return toPageResponse(inquiryRepository.findMyInquiries(memberId, pageRequestDTO.getPageable(LATEST)), pageRequestDTO);
	}

	@Override
	public PageResponseDTO<InquiryDTO> getInquiries(InquirySearchDTO inquirySearchDTO) {
		return toPageResponse(inquiryRepository.search(inquirySearchDTO.getStatus(), inquirySearchDTO.getPageable(LATEST)),
				inquirySearchDTO);
	}

	@Override
	@Transactional
	public InquiryDTO answer(Long id, String answer) {
		Inquiry inquiry = findInquiry(id);
		boolean firstAnswer = inquiry.getStatus() == InquiryStatus.WAITING;
		inquiry.answer(answer, LocalDateTime.now());

		// 알림은 첫 답변 때만 (답변을 고칠 때마다 알림이 가지 않게)
		if (firstAnswer) {
			notificationService.send(inquiry.getMember(), NotificationType.INQUIRY_ANSWERED,
					"1:1 문의에 답변이 등록됐어요: " + inquiry.getTitle(), null);
		}
		return entityToDTO(inquiry);
	}

	@Override
	@Transactional
	public void remove(Long id) {
		inquiryRepository.delete(findInquiry(id));
	}

	private Inquiry findInquiry(Long id) {
		return inquiryRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "문의를 찾을 수 없습니다."));
	}

	private PageResponseDTO<InquiryDTO> toPageResponse(Page<Inquiry> result, PageRequestDTO pageRequestDTO) {
		return PageResponseDTO.<InquiryDTO>withAll()
				.dtoList(result.getContent().stream().map(this::entityToDTO).toList())
				.pageRequestDTO(pageRequestDTO)
				.totalCount(result.getTotalElements())
				.build();
	}
}
