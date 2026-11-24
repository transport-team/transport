package com.transport.util;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.security.LoginMember;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// JWT 만들기 · 확인 (비밀키 · 유효 시간은 app.jwt.*)
// 액세스 토큰 = 회원 id · 이메일 · 역할, 리프레시 토큰 = 회원 id만 (type 값으로 구분)
@Component
public class JWTUtil {

	private static final String TYPE = "type";
	private static final String ACCESS = "access";
	private static final String REFRESH = "refresh";

	private final SecretKey key;
	private final long accessMinutes;
	private final long refreshMinutes;

	public JWTUtil(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.access-minutes}") long accessMinutes,
			@Value("${app.jwt.refresh-minutes}") long refreshMinutes) {
		this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));  // 32바이트보다 짧으면 서버가 시작되지 않는다
		this.accessMinutes = accessMinutes;
		this.refreshMinutes = refreshMinutes;
	}

	public String createAccessToken(Member member) {
		return Jwts.builder()
				.subject(String.valueOf(member.getId()))
				.claim("email", member.getEmail())
				.claim("role", member.getRole().name())
				.claim(TYPE, ACCESS)
				.issuedAt(new Date())
				.expiration(expiresAt(accessMinutes))
				.signWith(key)
				.compact();
	}

	public String createRefreshToken(Member member) {
		return Jwts.builder()
				.subject(String.valueOf(member.getId()))
				.claim(TYPE, REFRESH)
				.issuedAt(new Date())
				.expiration(expiresAt(refreshMinutes))
				.signWith(key)
				.compact();
	}

	// 액세스 토큰 → 로그인 사용자. 만료되면 TOKEN_EXPIRED, 그 밖의 문제는 INVALID_TOKEN
	public LoginMember parseAccessToken(String token) {
		Claims claims = parse(token, ACCESS);
		return new LoginMember(Long.valueOf(claims.getSubject()), claims.get("email", String.class),
				MemberRole.valueOf(claims.get("role", String.class)));
	}

	// 리프레시 토큰 → 회원 id
	public Long parseRefreshToken(String token) {
		return Long.valueOf(parse(token, REFRESH).getSubject());
	}

	private Claims parse(String token, String type) {
		try {
			Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
			if (!type.equals(claims.get(TYPE, String.class))) {
				throw new CustomException(ErrorCode.INVALID_TOKEN);  // 액세스 · 리프레시 토큰을 바꿔 쓰지 못하게
			}
			return claims;
		} catch (ExpiredJwtException e) {
			throw new CustomException(ErrorCode.TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException e) {
			throw new CustomException(ErrorCode.INVALID_TOKEN);
		}
	}

	private Date expiresAt(long minutes) {
		return Date.from(Instant.now().plus(minutes, ChronoUnit.MINUTES));
	}
}
