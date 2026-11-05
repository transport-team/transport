package com.transport.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.transport.domain.Member;
import com.transport.domain.Term;
import com.transport.domain.TermAgreement;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.MemberDTO;
import com.transport.dto.MemberModifyDTO;
import com.transport.dto.MemberSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.PasswordChangeDTO;
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
	private final DriverService driverService;

	@Override
	public Long signup(SignupRequestDTO signupRequestDTO) {
		checkRole(signupRequestDTO);
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

		if (member.getRole() == MemberRole.DRIVER) {  // 승인 대기 기사 (같은 트랜잭션이라 실패하면 회원도 저장되지 않는다)
			driverService.register(member, signupRequestDTO.getLicenseType(), signupRequestDTO.getCareerYears());
		}

		log.info("회원가입: id={}, role={}", member.getId(), member.getRole());
		return member.getId();
	}

	@Override
	@Transactional(readOnly = true)
	public MemberDTO getMember(Long memberId) {
		return entityToDTO(findMember(memberId));
	}

	@Override
	public MemberDTO modify(Long memberId, MemberModifyDTO memberModifyDTO) {
		Member member = findMember(memberId);
		member.changePhone(memberModifyDTO.getPhone());
		return entityToDTO(member);
	}

	@Override
	public void changePassword(Long memberId, PasswordChangeDTO passwordChangeDTO) {
		Member member = findMember(memberId);
		if (!passwordEncoder.matches(passwordChangeDTO.getCurrentPassword(), member.getPassword())) {
			throw new CustomException(ErrorCode.WRONG_PASSWORD);
		}
		if (passwordEncoder.matches(passwordChangeDTO.getNewPassword(), member.getPassword())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "새 비밀번호가 지금 비밀번호와 같습니다.");
		}
		member.changePassword(passwordEncoder.encode(passwordChangeDTO.getNewPassword()));
		log.info("비밀번호 변경: id={}", memberId);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponseDTO<MemberDTO> getMembers(MemberSearchDTO memberSearchDTO) {
		Page<Member> result = memberRepository.search(memberSearchDTO.getRole(), memberSearchDTO.getEnabled(),
				memberSearchDTO.getKeyword(), memberSearchDTO.getPageable(Sort.by("id").descending()));
		List<MemberDTO> dtoList = result.getContent().stream()
				.map(this::entityToDTO)
				.toList();

		return PageResponseDTO.<MemberDTO>withAll()
				.dtoList(dtoList)
				.pageRequestDTO(memberSearchDTO)
				.totalCount(result.getTotalElements())
				.build();
	}

	@Override
	public MemberDTO changeEnabled(Long memberId, boolean enabled) {
		Member member = findMember(memberId);
		if (member.getRole() == MemberRole.ADMIN) {  // 관리자가 한 명뿐일 때 스스로 잠기지 않게
			throw new CustomException(ErrorCode.INVALID_INPUT, "관리자 계정의 상태는 바꿀 수 없습니다.");
		}
		member.changeEnabled(enabled);  // 변경 감지로 저장 (트랜잭션이 끝날 때 UPDATE)

		log.info("회원 상태 변경: id={}, enabled={}", memberId, enabled);
		return entityToDTO(member);
	}

	private Member findMember(Long memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
	}

	// 관리자는 가입할 수 없고, 기사는 면허 종류 · 경력이 있어야 한다
	private void checkRole(SignupRequestDTO signupRequestDTO) {
		MemberRole role = signupRequestDTO.getRole();
		if (role == MemberRole.ADMIN) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "관리자는 가입할 수 없습니다.");
		}
		if (role == MemberRole.DRIVER
				&& (!StringUtils.hasText(signupRequestDTO.getLicenseType()) || signupRequestDTO.getCareerYears() == null)) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "기사는 면허 종류와 경력을 입력해 주세요.");
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
