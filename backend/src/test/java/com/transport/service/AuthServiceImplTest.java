package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.LoginRequestDTO;
import com.transport.dto.LoginResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.MemberRepository;
import com.transport.util.JWTUtil;

// 로그인 · 재발급 규칙 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private DriverService driverService;

	private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

	private final JWTUtil jwtUtil = new JWTUtil("test-secret-key-for-jwt-signing-32bytes-or-more", 30, 1440);

	private AuthServiceImpl authService;

	@BeforeEach
	void setUp() {
		authService = new AuthServiceImpl(memberRepository, passwordEncoder, jwtUtil, driverService);
	}

	@Test
	void approvedDriverCanLogin() {
		when(memberRepository.findByEmail("driver@test.com")).thenReturn(Optional.of(driver()));
		when(driverService.getStatus(5L)).thenReturn(DriverStatus.ACTIVE);

		LoginResponseDTO response = authService.login(new LoginRequestDTO("driver@test.com", "password1!"));

		assertThat(response.getRole()).isEqualTo(MemberRole.DRIVER);
	}

	@Test
	void pendingDriverCannotLogin() {
		when(memberRepository.findByEmail("driver@test.com")).thenReturn(Optional.of(driver()));
		when(driverService.getStatus(5L)).thenReturn(DriverStatus.PENDING);

		assertErrorCode(() -> authService.login(new LoginRequestDTO("driver@test.com", "password1!")), ErrorCode.DRIVER_NOT_APPROVED);
	}

	@Test
	void stoppedDriverCannotLogin() {
		when(memberRepository.findByEmail("driver@test.com")).thenReturn(Optional.of(driver()));
		when(driverService.getStatus(5L)).thenReturn(DriverStatus.INACTIVE);

		assertErrorCode(() -> authService.login(new LoginRequestDTO("driver@test.com", "password1!")), ErrorCode.ACCOUNT_DISABLED);
	}

	@Test
	void login() {
		when(memberRepository.findByEmail("kim@test.com")).thenReturn(Optional.of(member(true)));

		LoginResponseDTO response = authService.login(new LoginRequestDTO("kim@test.com", "password1!"));

		assertThat(response.getMemberId()).isEqualTo(4L);
		assertThat(response.getName()).isEqualTo("김고객");
		assertThat(response.getRole()).isEqualTo(MemberRole.CUSTOMER);
		assertThat(jwtUtil.parseAccessToken(response.getAccessToken()).getId()).isEqualTo(4L);
		assertThat(jwtUtil.parseRefreshToken(response.getRefreshToken())).isEqualTo(4L);
	}

	@Test
	void wrongPassword() {
		when(memberRepository.findByEmail("kim@test.com")).thenReturn(Optional.of(member(true)));

		assertErrorCode(() -> authService.login(new LoginRequestDTO("kim@test.com", "wrong-password")), ErrorCode.LOGIN_FAILED);
	}

	@Test
	void unknownEmail() {
		when(memberRepository.findByEmail("nobody@test.com")).thenReturn(Optional.empty());

		assertErrorCode(() -> authService.login(new LoginRequestDTO("nobody@test.com", "password1!")), ErrorCode.LOGIN_FAILED);
	}

	@Test
	void disabledMemberCannotLogin() {
		when(memberRepository.findByEmail("kim@test.com")).thenReturn(Optional.of(member(false)));

		assertErrorCode(() -> authService.login(new LoginRequestDTO("kim@test.com", "password1!")), ErrorCode.ACCOUNT_DISABLED);
	}

	@Test
	void refresh() {
		when(memberRepository.findById(4L)).thenReturn(Optional.of(member(true)));

		LoginResponseDTO response = authService.refresh(jwtUtil.createRefreshToken(member(true)));

		assertThat(jwtUtil.parseAccessToken(response.getAccessToken()).getId()).isEqualTo(4L);
		assertThat(jwtUtil.parseRefreshToken(response.getRefreshToken())).isEqualTo(4L);
	}

	@Test
	void refreshWithAccessToken() {
		assertErrorCode(() -> authService.refresh(jwtUtil.createAccessToken(member(true))), ErrorCode.INVALID_TOKEN);
	}

	@Test
	void disabledMemberCannotRefresh() {
		when(memberRepository.findById(4L)).thenReturn(Optional.of(member(false)));

		assertErrorCode(() -> authService.refresh(jwtUtil.createRefreshToken(member(false))), ErrorCode.ACCOUNT_DISABLED);
	}

	private Member member(boolean enabled) {
		return Member.builder()
				.id(4L)
				.email("kim@test.com")
				.password(passwordEncoder.encode("password1!"))
				.name("김고객")
				.phone("010-1234-5678")
				.role(MemberRole.CUSTOMER)
				.enabled(enabled)
				.build();
	}

	private Member driver() {
		return Member.builder()
				.id(5L)
				.email("driver@test.com")
				.password(passwordEncoder.encode("password1!"))
				.name("김기사")
				.phone("010-2222-3333")
				.role(MemberRole.DRIVER)
				.enabled(true)
				.build();
	}

	private void assertErrorCode(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(errorCode);
	}
}
