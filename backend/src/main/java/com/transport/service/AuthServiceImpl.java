package com.transport.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.LoginRequestDTO;
import com.transport.dto.LoginResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.MemberRepository;
import com.transport.util.JWTUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// AuthService 구현 (수업의 formLogin 대신 JSON 로그인 API — 비밀번호는 여기서 확인)
@Service
@Transactional(readOnly = true)
@Log4j2
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JWTUtil jwtUtil;
	private final DriverService driverService;

	@Override
	public LoginResponseDTO login(LoginRequestDTO loginRequestDTO) {
		// 없는 이메일 · 틀린 비밀번호는 같은 에러 (가입 여부를 알려 주지 않는다)
		Member member = memberRepository.findByEmail(loginRequestDTO.getEmail())
				.filter(found -> passwordEncoder.matches(loginRequestDTO.getPassword(), found.getPassword()))
				.orElseThrow(() -> new CustomException(ErrorCode.LOGIN_FAILED));
		checkCanLogin(member);

		log.info("로그인: id={}, role={}", member.getId(), member.getRole());
		return issueTokens(member);
	}

	@Override
	public LoginResponseDTO refresh(String refreshToken) {
		Long memberId = jwtUtil.parseRefreshToken(refreshToken);
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.INVALID_TOKEN));
		checkCanLogin(member);  // 그사이 비활성화 · 중지된 회원은 재발급도 막는다
		return issueTokens(member);
	}

	// 비활성 계정은 막고, 기사는 승인(ACTIVE)된 경우만 (승인 대기 → DRIVER_NOT_APPROVED, 중지 → ACCOUNT_DISABLED)
	private void checkCanLogin(Member member) {
		if (!member.isEnabled()) {
			throw new CustomException(ErrorCode.ACCOUNT_DISABLED);
		}
		if (member.getRole() == MemberRole.DRIVER) {
			DriverStatus status = driverService.getStatus(member.getId());
			if (status == DriverStatus.PENDING) {
				throw new CustomException(ErrorCode.DRIVER_NOT_APPROVED);
			}
			if (status == DriverStatus.INACTIVE) {
				throw new CustomException(ErrorCode.ACCOUNT_DISABLED);
			}
		}
	}

	private LoginResponseDTO issueTokens(Member member) {
		return LoginResponseDTO.builder()
				.accessToken(jwtUtil.createAccessToken(member))
				.refreshToken(jwtUtil.createRefreshToken(member))
				.memberId(member.getId())
				.email(member.getEmail())
				.name(member.getName())
				.role(member.getRole())
				.build();
	}
}
