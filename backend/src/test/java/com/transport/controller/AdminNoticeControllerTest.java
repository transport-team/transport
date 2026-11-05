package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import com.transport.dto.NoticeRequestDTO;
import com.transport.service.NoticeService;
import com.transport.util.JWTUtil;

// 관리자 공지 API: 관리자만 · 입력 검사 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = AdminNoticeController.class, properties = "app.jwt.secret=" + AdminNoticeControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class AdminNoticeControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";
	private static final String BODY = "{\"title\": \"추석 연휴 배송 안내\", \"content\": \"배차가 늦어질 수 있어요.\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private NoticeService noticeService;

	@Test
	void registerForAdmin() throws Exception {
		when(noticeService.register(eq(1L), any(NoticeRequestDTO.class))).thenReturn(5L);

		mockMvc.perform(post("/api/admin/notices").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.noticeId").value(5));
	}

	@Test
	void customerIsForbidden() throws Exception {
		mockMvc.perform(post("/api/admin/notices").header(HttpHeaders.AUTHORIZATION, token(MemberRole.CUSTOMER))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void titleRequired() throws Exception {
		mockMvc.perform(post("/api/admin/notices").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"내용만 있어요\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("제목을 입력해 주세요."));
	}

	@Test
	void removeForAdmin() throws Exception {
		mockMvc.perform(delete("/api/admin/notices/5").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isNoContent());
		verify(noticeService).remove(5L);
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("user@test.com").role(role).build());
	}
}
