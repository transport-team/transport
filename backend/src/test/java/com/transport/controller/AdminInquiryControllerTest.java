package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import com.transport.domain.enums.InquiryStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.InquiryDTO;
import com.transport.dto.InquirySearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.InquiryService;
import com.transport.util.JWTUtil;

// 관리자 1:1 문의 API: 상태 필터 · 답변 · 삭제는 관리자만, 입력 검사 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = AdminInquiryController.class, properties = "app.jwt.secret=" + AdminInquiryControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class AdminInquiryControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";
	private static final String ANSWER = "{\"answer\": \"오늘 오후에 배차돼요.\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private InquiryService inquiryService;

	@Test
	void listByStatus() throws Exception {
		when(inquiryService.getInquiries(any(InquirySearchDTO.class))).thenReturn(PageResponseDTO.<InquiryDTO>withAll()
				.dtoList(List.of(InquiryDTO.builder().id(7L).status(InquiryStatus.WAITING).memberName("김고객").memberRole(MemberRole.CUSTOMER).build()))
				.pageRequestDTO(PageRequestDTO.builder().build())
				.totalCount(1)
				.build());

		mockMvc.perform(get("/api/admin/inquiries").param("status", "WAITING").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dtoList[0].memberName").value("김고객"))
				.andExpect(jsonPath("$.dtoList[0].memberRole").value("CUSTOMER"));
		verify(inquiryService).getInquiries(argThat(search -> search.getStatus() == InquiryStatus.WAITING));
	}

	@Test
	void answer() throws Exception {
		when(inquiryService.answer(7L, "오늘 오후에 배차돼요.")).thenReturn(
				InquiryDTO.builder().id(7L).status(InquiryStatus.ANSWERED).answer("오늘 오후에 배차돼요.").build());

		mockMvc.perform(patch("/api/admin/inquiries/7/answer").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(ANSWER))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ANSWERED"));
	}

	@Test
	void blankAnswer() throws Exception {
		mockMvc.perform(patch("/api/admin/inquiries/7/answer").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{\"answer\": \"  \"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("답변을 입력해 주세요."));
	}

	@Test
	void customerIsForbidden() throws Exception {
		mockMvc.perform(patch("/api/admin/inquiries/7/answer").header(HttpHeaders.AUTHORIZATION, token(MemberRole.CUSTOMER))
						.contentType(MediaType.APPLICATION_JSON).content(ANSWER))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		verify(inquiryService, never()).answer(eq(7L), any());
	}

	@Test
	void remove() throws Exception {
		mockMvc.perform(delete("/api/admin/inquiries/7").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isNoContent());
		verify(inquiryService).remove(7L);
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("user@test.com").role(role).build());
	}
}
