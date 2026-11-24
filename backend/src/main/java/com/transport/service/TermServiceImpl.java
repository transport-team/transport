package com.transport.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Term;
import com.transport.dto.TermAdminDTO;
import com.transport.dto.TermDTO;
import com.transport.dto.TermRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.TermAgreementRepository;
import com.transport.repository.TermAgreementRepository.TermAgreementCount;
import com.transport.repository.TermRepository;

import lombok.RequiredArgsConstructor;

// TermService 구현
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

	private final TermRepository termRepository;
	private final TermAgreementRepository termAgreementRepository;

	@Override
	public List<TermDTO> getActiveTerms() {
		return termRepository.findAllByActiveTrueOrderByRequiredDescIdAsc().stream()
				.map(this::entityToDTO)
				.toList();
	}

	@Override
	public List<TermAdminDTO> getAllTerms() {
		Map<Long, Long> agreementCounts = termAgreementRepository.countGroupByTerm().stream()
				.collect(Collectors.toMap(TermAgreementCount::getTermId, TermAgreementCount::getAgreementCount));

		return termRepository.findAllByOrderByActiveDescRequiredDescIdAsc().stream()
				.map(term -> entityToAdminDTO(term, agreementCounts.getOrDefault(term.getId(), 0L)))
				.toList();
	}

	@Override
	@Transactional
	public Long register(TermRequestDTO termRequestDTO) {
		String title = termRequestDTO.getTitle().strip();
		if (termRepository.existsByTitleAndActiveTrue(title)) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "같은 이름의 약관이 이미 사용 중이에요. 내용을 바꾸려면 그 약관을 개정해 주세요.");
		}
		return termRepository.save(toEntity(termRequestDTO)).getId();
	}

	@Override
	@Transactional
	public Long revise(Long id, TermRequestDTO termRequestDTO) {
		Term current = findTerm(id);
		if (!current.isActive()) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "사용 중인 약관만 개정할 수 있어요.");
		}
		if (current.getVersion().equals(termRequestDTO.getVersion().strip())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "새 버전은 지금 버전(" + current.getVersion() + ")과 달라야 해요.");
		}
		if (termRepository.existsByTitleAndActiveTrueAndIdNot(termRequestDTO.getTitle().strip(), id)) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "같은 이름의 약관이 이미 사용 중이에요. 다른 이름을 써 주세요.");
		}

		current.changeActive(false);  // 이전 버전은 동의 이력과 함께 남는다
		return termRepository.save(toEntity(termRequestDTO)).getId();
	}

	@Override
	@Transactional
	public TermAdminDTO changeActive(Long id, boolean active) {
		Term term = findTerm(id);
		if (active && !term.isActive() && termRepository.existsByTitleAndActiveTrueAndIdNot(term.getTitle(), id)) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "같은 이름의 약관이 이미 사용 중이에요. 그 약관을 먼저 사용 중지해 주세요.");
		}
		term.changeActive(active);
		return entityToAdminDTO(term, termAgreementRepository.countByTermId(id));
	}

	@Override
	@Transactional
	public void remove(Long id) {
		Term term = findTerm(id);
		if (termAgreementRepository.countByTermId(id) > 0) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "동의한 회원이 있는 약관은 삭제할 수 없어요. 사용 중지해 주세요.");
		}
		termRepository.delete(term);
	}

	private Term findTerm(Long id) {
		return termRepository.findById(id)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "약관을 찾을 수 없습니다."));
	}

	// 등록 · 개정 모두 새 행으로 저장 (바로 사용 중)
	private Term toEntity(TermRequestDTO termRequestDTO) {
		return Term.builder()
				.title(termRequestDTO.getTitle().strip())
				.content(termRequestDTO.getContent())
				.required(termRequestDTO.getRequired())
				.version(termRequestDTO.getVersion().strip())
				.active(true)
				.build();
	}
}
