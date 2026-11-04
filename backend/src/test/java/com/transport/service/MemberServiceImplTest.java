package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

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
import com.transport.dto.MemberDTO;
import com.transport.dto.MemberModifyDTO;
import com.transport.dto.PasswordChangeDTO;
import com.transport.dto.SignupRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.MemberRepository;
import com.transport.repository.TermAgreementRepository;
import com.transport.repository.TermRepository;

// 회원가입 · 내 정보 수정 규칙 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private TermRepository termRepository;

	@Mock
	private TermAgreementRepository termAgreementRepository;

	@Mock
	private DriverService driverService;

	@Captor
	private ArgumentCaptor<Member> memberCaptor;

	@Captor
	private ArgumentCaptor<List<TermAgreement>> agreementCaptor;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private MemberServiceImpl memberService;

	@BeforeEach
	void setUp() {
		memberService = new MemberServiceImpl(memberRepository, termRepository, termAgreementRepository, passwordEncoder, driverService);
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
		verify(driverService, never()).register(any(), any(), anyInt());  // 고객은 기사 정보를 만들지 않는다
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
	void signupDriver() {
		givenTerms();
		when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

		memberService.signup(driverRequest("1종 보통", 5));

		verify(memberRepository).save(memberCaptor.capture());
		assertThat(memberCaptor.getValue().getRole()).isEqualTo(MemberRole.DRIVER);
		verify(driverService).register(any(Member.class), eq("1종 보통"), eq(5));  // 승인 대기 기사 생성
	}

	@Test
	void driverWithoutLicenseOrCareer() {
		assertThatThrownBy(() -> memberService.signup(driverRequest(" ", 5)))
				.isInstanceOf(CustomException.class)
				.hasMessage("기사는 면허 종류와 경력을 입력해 주세요.");
		assertThatThrownBy(() -> memberService.signup(driverRequest("1종 보통", null)))
				.isInstanceOf(CustomException.class)
				.hasMessage("기사는 면허 종류와 경력을 입력해 주세요.");
		verify(memberRepository, never()).save(any());
	}

	@Test
	void modifyPhone() {
		Member member = savedMember();
		when(memberRepository.findById(4L)).thenReturn(Optional.of(member));

		MemberDTO result = memberService.modify(4L, MemberModifyDTO.builder().phone("010-9999-8888").build());

		assertThat(result.getPhone()).isEqualTo("010-9999-8888");
		assertThat(member.getPhone()).isEqualTo("010-9999-8888");  // 트랜잭션이 끝날 때 변경 감지로 저장된다
	}

	@Test
	void changePassword() {
		Member member = savedMember();
		when(memberRepository.findById(4L)).thenReturn(Optional.of(member));

		memberService.changePassword(4L, passwordChange("password1!", "newPassword1!"));

		assertThat(passwordEncoder.matches("newPassword1!", member.getPassword())).isTrue();
	}

	@Test
	void wrongCurrentPassword() {
		Member member = savedMember();
		when(memberRepository.findById(4L)).thenReturn(Optional.of(member));

		assertThatThrownBy(() -> memberService.changePassword(4L, passwordChange("wrong-password", "newPassword1!")))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.WRONG_PASSWORD);
		assertThat(passwordEncoder.matches("password1!", member.getPassword())).isTrue();  // 바뀌지 않는다
	}

	@Test
	void sameNewPassword() {
		when(memberRepository.findById(4L)).thenReturn(Optional.of(savedMember()));

		assertThatThrownBy(() -> memberService.changePassword(4L, passwordChange("password1!", "password1!")))
				.isInstanceOf(CustomException.class)
				.hasMessage("새 비밀번호가 지금 비밀번호와 같습니다.");
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

	private SignupRequestDTO driverRequest(String licenseType, Integer careerYears) {
		SignupRequestDTO request = request(MemberRole.DRIVER, List.of(1L, 2L));
		request.setLicenseType(licenseType);
		request.setCareerYears(careerYears);
		return request;
	}

	// 가입되어 있는 고객 (비밀번호 password1!)
	private Member savedMember() {
		return Member.builder().id(4L).email("kim@test.com").password(passwordEncoder.encode("password1!"))
				.name("김고객").phone("010-1234-5678").role(MemberRole.CUSTOMER).enabled(true).build();
	}

	private PasswordChangeDTO passwordChange(String currentPassword, String newPassword) {
		return PasswordChangeDTO.builder().currentPassword(currentPassword).newPassword(newPassword).build();
	}
}
