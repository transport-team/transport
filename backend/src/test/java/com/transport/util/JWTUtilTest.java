package com.transport.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.security.LoginMember;

// 토큰 만들기 · 확인 (만료 · 위조 · 종류 바꿔 쓰기)
class JWTUtilTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	private final JWTUtil jwtUtil = new JWTUtil(SECRET, 30, 1440);

	private final Member member = Member.builder().id(4L).email("kim@test.com").role(MemberRole.CUSTOMER).build();

	@Test
	void accessToken() {
		LoginMember loginMember = jwtUtil.parseAccessToken(jwtUtil.createAccessToken(member));

		assertThat(loginMember.getId()).isEqualTo(4L);
		assertThat(loginMember.getEmail()).isEqualTo("kim@test.com");
		assertThat(loginMember.getRole()).isEqualTo(MemberRole.CUSTOMER);
		assertThat(loginMember.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_CUSTOMER");
	}

	@Test
	void refreshToken() {
		assertThat(jwtUtil.parseRefreshToken(jwtUtil.createRefreshToken(member))).isEqualTo(4L);
	}

	@Test
	void expiredToken() {
		JWTUtil expiredUtil = new JWTUtil(SECRET, -1, -1);  // 유효 시간이 이미 지난 토큰을 만든다

		assertErrorCode(() -> jwtUtil.parseAccessToken(expiredUtil.createAccessToken(member)), ErrorCode.TOKEN_EXPIRED);
		assertErrorCode(() -> jwtUtil.parseRefreshToken(expiredUtil.createRefreshToken(member)), ErrorCode.TOKEN_EXPIRED);
	}

	@Test
	void tokenTypesCannotBeSwapped() {
		assertErrorCode(() -> jwtUtil.parseAccessToken(jwtUtil.createRefreshToken(member)), ErrorCode.INVALID_TOKEN);
		assertErrorCode(() -> jwtUtil.parseRefreshToken(jwtUtil.createAccessToken(member)), ErrorCode.INVALID_TOKEN);
	}

	@Test
	void forgedOrBrokenToken() {
		JWTUtil otherKeyUtil = new JWTUtil("another-secret-key-for-jwt-signing-32bytes", 30, 1440);

		assertErrorCode(() -> jwtUtil.parseAccessToken(otherKeyUtil.createAccessToken(member)), ErrorCode.INVALID_TOKEN);
		assertErrorCode(() -> jwtUtil.parseAccessToken("not.a.token"), ErrorCode.INVALID_TOKEN);
		assertErrorCode(() -> jwtUtil.parseAccessToken(""), ErrorCode.INVALID_TOKEN);
	}

	private void assertErrorCode(ThrowingCallable callable, ErrorCode errorCode) {
		assertThatThrownBy(callable)
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(errorCode);
	}
}
