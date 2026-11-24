package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import com.transport.dto.MemberDTO;
import com.transport.dto.MemberSearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.MemberService;
import com.transport.util.JWTUtil;

// 관리자 회원 API: 관리자만 · 입력 검사 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = AdminMemberController.class, properties = "app.jwt.secret=" + AdminMemberControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class AdminMemberControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private MemberService memberService;

	@Test
	void listForAdmin() throws Exception {
		when(memberService.getMembers(any(MemberSearchDTO.class))).thenReturn(PageResponseDTO.<MemberDTO>withAll()
				.dtoList(List.of(MemberDTO.builder().id(4L).name("김고객").role(MemberRole.CUSTOMER).enabled(true).build()))
				.pageRequestDTO(new PageRequestDTO())
				.totalCount(1)
				.build());

		mockMvc.perform(get("/api/admin/members").param("role", "CUSTOMER").param("enabled", "true").param("keyword", "김")
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dtoList[0].name").value("김고객"))
				.andExpect(jsonPath("$.dtoList[0].enabled").value(true))
				.andExpect(jsonPath("$.totalCount").value(1));
		verify(memberService).getMembers(argThat(search -> search.getRole() == MemberRole.CUSTOMER
				&& Boolean.TRUE.equals(search.getEnabled()) && "김".equals(search.getKeyword()) && search.getPage() == 1));
	}

	@Test
	void customerIsForbidden() throws Exception {
		mockMvc.perform(get("/api/admin/members").header(HttpHeaders.AUTHORIZATION, token(MemberRole.CUSTOMER)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void disable() throws Exception {
		when(memberService.changeEnabled(eq(4L), eq(false)))
				.thenReturn(MemberDTO.builder().id(4L).role(MemberRole.CUSTOMER).enabled(false).build());

		mockMvc.perform(patch("/api/admin/members/4/enabled").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{\"enabled\": false}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.enabled").value(false));
	}

	@Test
	void enabledRequired() throws Exception {
		mockMvc.perform(patch("/api/admin/members/4/enabled").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("변경할 상태를 선택해 주세요."));
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("user@test.com").role(role).build());
	}
}
