package com.transport.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

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

		// =========================================================================
		// 1. [현재 적용 중 - 개발용 설정]
		// REST API + React 연동 개발을 위해 모든 API 접근을 허용합니다.
		// =========================================================================
		http.authorizeHttpRequests(auth -> auth.anyRequest().permitAll());


		// =========================================================================
		// 2. [추후 실제 보안/JWT 적용 시 전환 예시]
		// 개발이 완료되고 JWT 필터를 적용할 때는 위의 permitAll() 대신 아래 구조로 전환합니다.
		// =========================================================================
		/*
		http.authorizeHttpRequests(auth -> auth
			// 헬스체크, 로그인/회원가입, Swagger 등 공개 API는 허용
			.requestMatchers("/api/health", "/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
			// 관리자 전용 API
			.requestMatchers("/api/admin/**").hasRole("ADMIN")
			// 기사 전용 API
			.requestMatchers("/api/driver/**").hasRole("DRIVER")
			// 그 외 모든 요청은 인증 필수
			.anyRequest().authenticated()
		);
		// .addFilterBefore(jwtCheckFilter, UsernamePasswordAuthenticationFilter.class);
		*/

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
