package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.transport.dto.FaqDTO;
import com.transport.dto.FaqRequestDTO;
import com.transport.service.FaqService;
import com.transport.util.JWTUtil;

// 관리자 FAQ API: 관리자만 · 입력 검사 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = AdminFaqController.class, properties = "app.jwt.secret=" + AdminFaqControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class AdminFaqControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";
	private static final String BODY =
			"{\"category\": \"회원\", \"question\": \"비밀번호를 바꾸려면?\", \"answer\": \"내 정보에서 바꿀 수 있어요.\", \"sortOrder\": 3, \"active\": true}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private FaqService faqService;

	@Test
	void listForAdmin() throws Exception {
		when(faqService.getAllFaqs()).thenReturn(List.of(FaqDTO.builder().id(3L).category("회원").active(false).build()));

		mockMvc.perform(get("/api/admin/faqs").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].active").value(false));  // 숨긴 FAQ도 보인다
	}

	@Test
	void registerForAdmin() throws Exception {
		when(faqService.register(any(FaqRequestDTO.class))).thenReturn(7L);

		mockMvc.perform(post("/api/admin/faqs").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.faqId").value(7));
	}

	@Test
	void customerIsForbidden() throws Exception {
		mockMvc.perform(post("/api/admin/faqs").header(HttpHeaders.AUTHORIZATION, token(MemberRole.CUSTOMER))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void unknownCategory() throws Exception {
		mockMvc.perform(post("/api/admin/faqs").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(BODY.replace("회원", "기타")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("분류는 배송 · 결제 · 회원 중에서 골라 주세요."));
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("user@test.com").role(role).build());
	}
}
