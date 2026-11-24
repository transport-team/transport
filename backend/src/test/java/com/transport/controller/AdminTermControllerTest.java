package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.transport.config.SecurityConfig;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.TermAdminDTO;
import com.transport.dto.TermRequestDTO;
import com.transport.service.TermService;
import com.transport.util.JWTUtil;

// 관리자 약관 API: 관리자만 · 입력 검사 · 응답 모양 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = AdminTermController.class, properties = "app.jwt.secret=" + AdminTermControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class AdminTermControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";
	private static final String BODY =
			"{\"title\": \"서비스 이용약관\", \"content\": \"바뀐 내용\", \"required\": true, \"version\": \"v1.1\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private TermService termService;

	@Test
	void listForAdmin() throws Exception {
		when(termService.getAllTerms()).thenReturn(List.of(
				TermAdminDTO.builder().id(1L).title("서비스 이용약관").version("v1.0").active(false).agreementCount(3).build()));

		mockMvc.perform(get("/api/admin/terms").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].active").value(false))       // 지난 버전도 보인다
				.andExpect(jsonPath("$[0].agreementCount").value(3));
	}

	@Test
	void registerForAdmin() throws Exception {
		when(termService.register(any(TermRequestDTO.class))).thenReturn(5L);

		mockMvc.perform(post("/api/admin/terms").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.termId").value(5));
	}

	@Test
	void reviseForAdmin() throws Exception {
		when(termService.revise(eq(1L), any(TermRequestDTO.class))).thenReturn(4L);

		mockMvc.perform(post("/api/admin/terms/1/revisions").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.termId").value(4));
	}

	@Test
	void customerIsForbidden() throws Exception {
		mockMvc.perform(post("/api/admin/terms").header(HttpHeaders.AUTHORIZATION, token(MemberRole.CUSTOMER))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void blankVersion() throws Exception {
		mockMvc.perform(post("/api/admin/terms").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(BODY.replace("v1.1", "")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("버전을 입력해 주세요."));
	}

	@Test
	void changeActiveNeedsValue() throws Exception {
		mockMvc.perform(patch("/api/admin/terms/1/active").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("변경할 상태를 선택해 주세요."));
	}

	@Test
	void removeForAdmin() throws Exception {
		mockMvc.perform(delete("/api/admin/terms/6").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isNoContent());
		verify(termService).remove(6L);
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("user@test.com").role(role).build());
	}
}
