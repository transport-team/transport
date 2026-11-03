package com.transport.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.transport.config.SecurityConfig;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.MemberDTO;
import com.transport.service.MemberService;
import com.transport.util.JWTUtil;

// 토큰 확인 필터 · 401 응답 (보안 필터를 켜고 내 정보 API로 확인)
@WebMvcTest(controllers = MemberController.class, properties = "app.jwt.secret=" + MemberControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class MemberControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private MemberService memberService;

	private final Member member = Member.builder().id(4L).email("kim@test.com").role(MemberRole.CUSTOMER).build();

	@Test
	void meWithToken() throws Exception {
		when(memberService.getMember(4L)).thenReturn(MemberDTO.builder()
				.id(4L).email("kim@test.com").name("김고객").phone("010-1234-5678").role(MemberRole.CUSTOMER).build());

		me("Bearer " + jwtUtil.createAccessToken(member))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(4))
				.andExpect(jsonPath("$.name").value("김고객"));
	}

	@Test
	void meWithoutToken() throws Exception {
		mockMvc.perform(get("/api/members/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void expiredToken() throws Exception {
		String expiredToken = new JWTUtil(SECRET, -1, -1).createAccessToken(member);

		me("Bearer " + expiredToken)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("TOKEN_EXPIRED"));
	}

	@Test
	void brokenToken() throws Exception {
		me("Bearer not.a.token")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	@Test
	void refreshTokenIsNotAccessToken() throws Exception {
		me("Bearer " + jwtUtil.createRefreshToken(member))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("INVALID_TOKEN"));
	}

	private ResultActions me(String authorization) throws Exception {
		return mockMvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, authorization));
	}
}
