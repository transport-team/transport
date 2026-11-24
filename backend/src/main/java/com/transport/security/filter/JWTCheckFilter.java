package com.transport.security.filter;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.transport.exception.CustomException;
import com.transport.security.LoginMember;
import com.transport.security.handler.ErrorResponseWriter;
import com.transport.util.JWTUtil;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 액세스 토큰을 확인해 로그인 사용자로 등록 (공통 파일 — 관리: 팀원 1)
// 토큰이 없으면 그냥 통과 → 막을지는 SecurityConfig의 URL 규칙 · @PreAuthorize가 정한다
// 만료된 토큰은 401 TOKEN_EXPIRED (프론트가 재발급 후 다시 요청), 잘못된 토큰은 401 INVALID_TOKEN
@Log4j2
@RequiredArgsConstructor
public class JWTCheckFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final JWTUtil jwtUtil;
	private final ErrorResponseWriter errorResponseWriter;

	// 로그인 · 가입 · 재발급은 만료된 토큰이 붙어 와도 막지 않는다
	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return request.getRequestURI().startsWith("/api/auth/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BEARER)) {
			filterChain.doFilter(request, response);
			return;
		}

		try {
			LoginMember loginMember = jwtUtil.parseAccessToken(header.substring(BEARER.length()));
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(new UsernamePasswordAuthenticationToken(loginMember, null, loginMember.getAuthorities()));
			SecurityContextHolder.setContext(context);
		} catch (CustomException e) {
			log.warn("토큰 확인 실패: {} {}", e.getErrorCode(), request.getRequestURI());
			errorResponseWriter.write(response, e.getErrorCode());
			return;
		}
		filterChain.doFilter(request, response);
	}
}
