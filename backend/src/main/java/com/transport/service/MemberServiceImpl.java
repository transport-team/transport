package com.transport.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Member;
import com.transport.domain.Term;
import com.transport.domain.TermAgreement;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.MemberDTO;
import com.transport.dto.SignupRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.MemberRepository;
import com.transport.repository.TermAgreementRepository;
import com.transport.repository.TermRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// MemberService 구현
@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

	private final MemberRepository memberRepository;
	private final TermRepository termRepository;
	private final TermAgreementRepository termAgreementRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public Long signup(SignupRequestDTO signupRequestDTO) {
		checkRole(signupRequestDTO.getRole());
		List<Term> agreedTerms = checkTerms(signupRequestDTO.getAgreedTermIds());
		if (memberRepository.existsByEmail(signupRequestDTO.getEmail())) {
			throw new CustomException(ErrorCode.DUPLICATE_EMAIL);
		}

		Member member = memberRepository.save(Member.builder()
				.email(signupRequestDTO.getEmail())
				.password(passwordEncoder.encode(signupRequestDTO.getPassword()))
				.name(signupRequestDTO.getName())
				.phone(signupRequestDTO.getPhone())
				.role(signupRequestDTO.getRole())
				.enabled(true)
				.build());

		LocalDateTime now = LocalDateTime.now();
		termAgreementRepository.saveAll(agreedTerms.stream()
				.map(term -> TermAgreement.builder().member(member).term(term).agreedAt(now).build())
				.toList());

		log.info("회원가입: id={}, role={}", member.getId(), member.getRole());
		return member.getId();
	}

	@Override
	@Transactional(readOnly = true)
	public MemberDTO getMember(Long memberId) {
		return memberRepository.findById(memberId)
				.map(this::entityToDTO)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	// 지금은 고객만 가입할 수 있다 (기사는 팀원 2의 DriverService가 나오면 J단계 PR 3에서 연결)
	private void checkRole(MemberRole role) {
		if (role == MemberRole.ADMIN) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "관리자는 가입할 수 없습니다.");
		}
		if (role == MemberRole.DRIVER) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "기사 가입은 준비 중입니다.");
		}
	}

	// 동의한 약관이 모두 지금 쓰는 약관인지, 필수 약관을 빠짐없이 동의했는지 확인 → 동의한 약관 목록
	private List<Term> checkTerms(List<Long> agreedTermIds) {
		Set<Long> agreedIds = new HashSet<>(agreedTermIds);
		List<Term> activeTerms = termRepository.findAllByActiveTrueOrderByIdAsc();

		List<Term> agreedTerms = activeTerms.stream()
				.filter(term -> agreedIds.contains(term.getId()))
				.toList();
		if (agreedTerms.size() != agreedIds.size()) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "동의할 수 없는 약관이 있습니다.");
		}

		boolean allRequiredAgreed = activeTerms.stream()
				.filter(Term::isRequired)
				.allMatch(term -> agreedIds.contains(term.getId()));
		if (!allRequiredAgreed) {
			throw new CustomException(ErrorCode.TERMS_REQUIRED);
		}
		return agreedTerms;
	}
}
