package com.transport.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.controller.HealthController;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.util.JWTUtil;

// URL 권한 · CORS 확인 (DB 없이 웹 계층만 띄운다)
// 권한 규칙은 테스트용 컨트롤러 주소로 확인한다: 통과하면 "ok", 막히면 401 · 403
@WebMvcTest(controllers = HealthController.class, properties = {
		"app.cors.allowed-origins=http://localhost:5173",
		"app.jwt.secret=test-secret-key-for-jwt-signing-32bytes-or-more"})
@Import({SecurityConfig.class, JWTUtil.class, SecurityConfigTest.TestController.class})
class SecurityConfigTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@Test
	void healthWithoutLogin() throws Exception {
		mockMvc.perform(get("/api/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ok"));
	}

	@Test
	void publicApiWithoutLogin() throws Exception {
		mockMvc.perform(post("/api/auth/login")).andExpect(status().isOk());
		mockMvc.perform(get("/api/terms")).andExpect(status().isOk());
		mockMvc.perform(get("/api/files/s_photo.jpg")).andExpect(status().isOk());
	}

	@Test
	void publicListIsGetOnly() throws Exception {
		mockMvc.perform(post("/api/terms"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void loginRequired() throws Exception {
		mockMvc.perform(get("/api/shipments/my"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void loginWithToken() throws Exception {
		mockMvc.perform(get("/api/shipments/my").header(HttpHeaders.AUTHORIZATION, bearer(MemberRole.CUSTOMER)))
				.andExpect(status().isOk())
				.andExpect(content().string("ok"));
	}

	@Test
	void adminOnly() throws Exception {
		mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION, bearer(MemberRole.CUSTOMER)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(get("/api/admin/test").header(HttpHeaders.AUTHORIZATION, bearer(MemberRole.ADMIN)))
				.andExpect(status().isOk());
	}

	@Test
	void driverOnly() throws Exception {
		mockMvc.perform(get("/api/driver/test").header(HttpHeaders.AUTHORIZATION, bearer(MemberRole.ADMIN)))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/driver/test").header(HttpHeaders.AUTHORIZATION, bearer(MemberRole.DRIVER)))
				.andExpect(status().isOk());
	}

	@Test
	void preflightToProtectedApi() throws Exception {
		// 브라우저가 토큰을 붙인 요청 전에 보내는 확인 요청(OPTIONS)은 로그인 없이 통과해야 한다
		mockMvc.perform(options("/api/shipments/my")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"));
	}

	@Test
	void preflightFromAllowedOrigin() throws Exception {
		mockMvc.perform(options("/api/health")
						.header(HttpHeaders.ORIGIN, "http://localhost:5173")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "PATCH")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:5173"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void preflightFromOtherOriginIsRejected() throws Exception {
		mockMvc.perform(options("/api/health")
						.header(HttpHeaders.ORIGIN, "http://evil.example.com")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
				.andExpect(status().isForbidden());
	}

	private String bearer(MemberRole role) {
		Member member = Member.builder().id(1L).email("test@test.com").role(role).build();
		return "Bearer " + jwtUtil.createAccessToken(member);
	}

	// 규칙 확인용 주소 (공개 · 로그인 · 관리자 · 기사)
	@RestController
	static class TestController {

		@PostMapping("/api/auth/login")
		String login() {
			return "ok";
		}

		@GetMapping({"/api/terms", "/api/files/{fileName}", "/api/shipments/my", "/api/admin/test", "/api/driver/test"})
		String ok() {
			return "ok";
		}

		@PostMapping("/api/terms")
		String postTerms() {
			return "ok";
		}
	}
}
