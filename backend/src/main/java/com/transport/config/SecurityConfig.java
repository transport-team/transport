package com.transport.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        // =========================================================================
        // 1. [현재 적용 중 - 개발용 설정]
        // REST API + React 연동 개발을 위해 모든 인증 차단을 해제한 상태입니다.
        // =========================================================================
        http
            .csrf(csrf -> csrf.disable()) // React/Axios 통신 시 CSRF 토큰 미사용을 위해 비활성화
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll()) // 모든 API 요청에 인증 필요 없음
            .formLogin(form -> form.disable()) // Spring Security 기본 HTML 로그인 폼 비활성화
            .httpBasic(basic -> basic.disable()); // 기본 HTTP Basic 인증 비활성화


        // =========================================================================
        // 2. [추후 실제 보안/JWT 적용 시 사용 예시]
        // 로그인/보안 기능 개발이 완료되면 위 1번 설정을 주석 처리하고 아래 코드를 활성화하세요.
        // =========================================================================
        /*
        http
            // CORS 설정 (프론트엔드 http://localhost:5173과의 통신 허용)
            .cors(cors -> cors.configure(http))
            
            // CSRF 비활성화 (REST API 환경에서는 보통 disable 처리)
            .csrf(csrf -> csrf.disable())
            
            // JWT 사용 시 세션을 생성하지 않음 (STATELESS 설정)
            .sessionManagement(session -> 
                session.sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS)
            )
            
            // URL별 권한 제어
            .authorizeHttpRequests(auth -> auth
                // 로그인, 회원가입, Swagger, 정적 리소스 등은 누구나 접근 가능
                .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                
                // 관리자 전용 API 권한 설정 (Role 기반)
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 기사 전용 API 권한 설정
                .requestMatchers("/api/driver/**").hasRole("DRIVER")
                
                // 그 외 모든 API 요청은 로그인(인증)된 사용자만 접근 가능
                .anyRequest().authenticated()
            )
            
            // 폼 로그인 및 기본 로그인 창 비활성화 (React 독립 로그인 화면 사용)
            .formLogin(form -> form.disable())
            .httpBasic(basic -> basic.disable());

            // 커스텀 JWT 필터 등록 예시 (UsernamePasswordAuthenticationFilter 이전에 실행)
            // .addFilterBefore(jwtAuthenticationFilter, org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);
        */

        return http.build();
    }

    /**
     * 비밀번호 암호화 빈(Bean) 등록
     * BCrypt 해시 함수를 사용하여 회원가입 및 로그인 시 비밀번호를 안전하게 암호화/비교합니다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}