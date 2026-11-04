package com.transport.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.transport.security.filter.JWTCheckFilter;
import com.transport.security.handler.CustomAccessDeniedHandler;
import com.transport.security.handler.CustomAuthenticationEntryPoint;
import com.transport.security.handler.ErrorResponseWriter;
import com.transport.util.JWTUtil;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

// 보안 · CORS 설정 (공통 파일 — 관리: 팀원 1)
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // 컨트롤러 메서드에 @PreAuthorize("hasRole('ADMIN')") 같은 권한 검사
@RequiredArgsConstructor
public class SecurityConfig {

	// 로그인 없이 쓰는 API (api.md 권한 "전체") — 새로 공개할 API는 팀원 1에게 요청
	private static final String[] PUBLIC_URLS = {
		"/api/auth/**",                                           // 가입 · 로그인 · 재발급
		"/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",  // Swagger
		"/error"                                                  // 에러 응답이 401로 바뀌지 않게
	};
	private static final String[] PUBLIC_GET_URLS = {
		"/api/health", "/api/terms", "/api/notices", "/api/notices/*", "/api/faqs",
		"/api/vehicle-types", "/api/cargo-types",
		"/api/files/*"  // 사진: <img>는 토큰을 붙일 수 없다 (UUID 파일명)
	};

	private final JWTUtil jwtUtil;
	private final JsonMapper jsonMapper;

	// yml에 설정이 없을 경우 기본값으로 localhost:5173, localhost:3000 지정 (서버 튕김 방지)
	@Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
	private List<String> allowedOrigins;

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		// CORS 및 백엔드 세션/폼 비활성화
		http
			.cors(cors -> cors.configurationSource(corsConfigurationSource()))
			.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

		// URL 권한 (2026-11-09 권한 전환): 공개 API 외에는 로그인, /api/admin/** 는 ADMIN, /api/driver/** 는 DRIVER
		// 그 밖의 역할 검사(예: 고객만 배송 신청)는 각 컨트롤러의 @PreAuthorize로 한다
		http.authorizeHttpRequests(auth -> auth
			.requestMatchers(PUBLIC_URLS).permitAll()
			.requestMatchers(HttpMethod.GET, PUBLIC_GET_URLS).permitAll()
			.requestMatchers("/api/admin/**").hasRole("ADMIN")
			.requestMatchers("/api/driver/**").hasRole("DRIVER")
			.anyRequest().authenticated());

		// 토큰 확인 (J단계): 토큰이 있으면 로그인 사용자로 등록, 없으면 통과. 401 · 403도 {code, message} 형식
		ErrorResponseWriter errorResponseWriter = new ErrorResponseWriter(jsonMapper);
		http.addFilterBefore(new JWTCheckFilter(jwtUtil, errorResponseWriter), UsernamePasswordAuthenticationFilter.class);
		http.exceptionHandling(exception -> exception
				.authenticationEntryPoint(new CustomAuthenticationEntryPoint(errorResponseWriter))
				.accessDeniedHandler(new CustomAccessDeniedHandler(errorResponseWriter)));

		return http.build();
	}

	/**
	 * 프론트엔드(React)와의 Cross-Origin Resource Sharing (CORS) 설정
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(allowedOrigins);
		configuration.setAllowedMethods(List.of("HEAD", "GET", "POST", "PUT", "PATCH", "DELETE"));
		configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
		configuration.setAllowCredentials(true);

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	/**
	 * 비밀번호 암호화 빈(Bean) 등록
	 */
	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
