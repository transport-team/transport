package com.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.transport.domain.Faq;
import com.transport.dto.FaqDTO;
import com.transport.dto.FaqRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.FaqRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// FaqService 구현
@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class FaqServiceImpl implements FaqService {

	private final FaqRepository faqRepository;

	@Override
	@Transactional(readOnly = true)
	public List<FaqDTO> getFaqs(String category) {
		List<Faq> faqs = StringUtils.hasText(category)
				? faqRepository.findByActiveTrueAndCategoryOrderBySortOrderAscIdAsc(category)
				: faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc();
		return faqs.stream().map(this::entityToDTO).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public List<FaqDTO> getAllFaqs() {
		return faqRepository.findAllByOrderBySortOrderAscIdAsc().stream()
				.map(this::entityToDTO)
				.toList();
	}

	@Override
	public Long register(FaqRequestDTO faqRequestDTO) {
		Faq faq = faqRepository.save(Faq.builder()
				.category(faqRequestDTO.getCategory())
				.question(faqRequestDTO.getQuestion())
				.answer(faqRequestDTO.getAnswer())
				.sortOrder(faqRequestDTO.getSortOrder())
				.active(faqRequestDTO.getActive())
				.build());

		log.info("FAQ 작성: id={}, category={}", faq.getId(), faq.getCategory());
		return faq.getId();
	}

	@Override
	public FaqDTO modify(Long faqId, FaqRequestDTO faqRequestDTO) {
		Faq faq = findFaq(faqId);
		faq.change(faqRequestDTO.getCategory(), faqRequestDTO.getQuestion(), faqRequestDTO.getAnswer(),
				faqRequestDTO.getSortOrder(), faqRequestDTO.getActive());
		return entityToDTO(faq);
	}

	@Override
	public void remove(Long faqId) {
		faqRepository.delete(findFaq(faqId));
		log.info("FAQ 삭제: id={}", faqId);
	}

	private Faq findFaq(Long faqId) {
		return faqRepository.findById(faqId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "FAQ를 찾을 수 없습니다."));
	}
}
