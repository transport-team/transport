package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.domain.Member;
import com.transport.domain.Term;
import com.transport.domain.TermAgreement;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.SignupRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.MemberRepository;
import com.transport.repository.TermAgreementRepository;
import com.transport.repository.TermRepository;

// 회원가입 규칙 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private TermRepository termRepository;

	@Mock
	private TermAgreementRepository termAgreementRepository;

	@Captor
	private ArgumentCaptor<Member> memberCaptor;

	@Captor
	private ArgumentCaptor<List<TermAgreement>> agreementCaptor;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private MemberServiceImpl memberService;

	@BeforeEach
	void setUp() {
		memberService = new MemberServiceImpl(memberRepository, termRepository, termAgreementRepository, passwordEncoder);
	}

	@Test
	void signupCustomer() {
		givenTerms();
		when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> {
			Member member = invocation.getArgument(0);
			ReflectionTestUtils.setField(member, "id", 4L);
			return member;
		});

		Long memberId = memberService.signup(request(MemberRole.CUSTOMER, List.of(1L, 2L, 3L)));

		assertThat(memberId).isEqualTo(4L);
		verify(memberRepository).save(memberCaptor.capture());
		Member saved = memberCaptor.getValue();
		assertThat(passwordEncoder.matches("password1!", saved.getPassword())).isTrue();
		assertThat(saved.getRole()).isEqualTo(MemberRole.CUSTOMER);
		assertThat(saved.isEnabled()).isTrue();

		verify(termAgreementRepository).saveAll(agreementCaptor.capture());
		assertThat(agreementCaptor.getValue())
				.extracting(agreement -> agreement.getTerm().getId())
				.containsExactly(1L, 2L, 3L);
	}

	@Test
	void duplicateEmail() {
		givenTerms();
		when(memberRepository.existsByEmail("kim@test.com")).thenReturn(true);

		assertThatThrownBy(() -> memberService.signup(request(MemberRole.CUSTOMER, List.of(1L, 2L))))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_EMAIL);
		verify(memberRepository, never()).save(any());
	}

	@Test
	void missingRequiredTerm() {
		givenTerms();

		assertThatThrownBy(() -> memberService.signup(request(MemberRole.CUSTOMER, List.of(1L, 3L))))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.TERMS_REQUIRED);
		verify(memberRepository, never()).save(any());
	}

	@Test
	void unknownTerm() {
		givenTerms();

		assertThatThrownBy(() -> memberService.signup(request(MemberRole.CUSTOMER, List.of(1L, 2L, 99L))))
				.isInstanceOf(CustomException.class)
				.hasMessage("동의할 수 없는 약관이 있습니다.")
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
	}

	@Test
	void adminCannotSignup() {
		assertThatThrownBy(() -> memberService.signup(request(MemberRole.ADMIN, List.of(1L, 2L))))
				.isInstanceOf(CustomException.class)
				.hasMessage("관리자는 가입할 수 없습니다.");
	}

	@Test
	void driverSignupNotReady() {
		assertThatThrownBy(() -> memberService.signup(request(MemberRole.DRIVER, List.of(1L, 2L))))
				.isInstanceOf(CustomException.class)
				.hasMessage("기사 가입은 준비 중입니다.");
	}

	// 필수 1 · 2, 선택 3 (data.sql 시드와 같은 구성)
	private void givenTerms() {
		when(termRepository.findAllByActiveTrueOrderByIdAsc())
				.thenReturn(List.of(term(1L, true), term(2L, true), term(3L, false)));
	}

	private Term term(Long id, boolean required) {
		return Term.builder().id(id).title("약관 " + id).content("내용").required(required).version("v1.0").active(true).build();
	}

	private SignupRequestDTO request(MemberRole role, List<Long> agreedTermIds) {
		return SignupRequestDTO.builder()
				.email("kim@test.com")
				.password("password1!")
				.name("김고객")
				.phone("010-1234-5678")
				.role(role)
				.agreedTermIds(agreedTermIds)
				.build();
	}
}
