package com.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.dto.TermDTO;
import com.transport.repository.TermRepository;

import lombok.RequiredArgsConstructor;

// TermService 구현
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TermServiceImpl implements TermService {

	private final TermRepository termRepository;

	@Override
	public List<TermDTO> getActiveTerms() {
		return termRepository.findAllByActiveTrueOrderByIdAsc().stream()
				.map(this::entityToDTO)
				.toList();
	}
}
