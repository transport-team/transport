package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import com.transport.domain.enums.InquiryStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.InquiryDTO;
import com.transport.dto.InquiryRequestDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.InquiryService;
import com.transport.util.JWTUtil;

// 1:1 문의 API (고객 · 기사): 로그인한 회원 id로 등록 · 조회, 관리자는 403 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = InquiryController.class, properties = "app.jwt.secret=" + InquiryControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class InquiryControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";
	private static final String BODY = "{\"title\": \"배차가 늦어요\", \"content\": \"언제 배차되나요?\"}";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private InquiryService inquiryService;

	@Test
	void customerRegisters() throws Exception {
		when(inquiryService.register(eq(2L), any(InquiryRequestDTO.class))).thenReturn(7L);

		mockMvc.perform(post("/api/inquiries").header(HttpHeaders.AUTHORIZATION, token(2L, MemberRole.CUSTOMER))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.inquiryId").value(7));
	}

	@Test
	void driverSeesMyInquiries() throws Exception {
		when(inquiryService.getMyInquiries(eq(3L), any(PageRequestDTO.class))).thenReturn(PageResponseDTO.<InquiryDTO>withAll()
				.dtoList(List.of(InquiryDTO.builder().id(8L).title("정산 문의").status(InquiryStatus.ANSWERED).answer("다음 주에 지급돼요.").build()))
				.pageRequestDTO(PageRequestDTO.builder().build())
				.totalCount(1)
				.build());

		mockMvc.perform(get("/api/inquiries/my").param("page", "1").header(HttpHeaders.AUTHORIZATION, token(3L, MemberRole.DRIVER)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dtoList[0].status").value("ANSWERED"))
				.andExpect(jsonPath("$.dtoList[0].answer").value("다음 주에 지급돼요."));
	}

	@Test
	void adminCannotRegister() throws Exception {
		mockMvc.perform(post("/api/inquiries").header(HttpHeaders.AUTHORIZATION, token(1L, MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		verify(inquiryService, never()).register(any(), any());
	}

	@Test
	void blankTitle() throws Exception {
		mockMvc.perform(post("/api/inquiries").header(HttpHeaders.AUTHORIZATION, token(2L, MemberRole.CUSTOMER))
						.contentType(MediaType.APPLICATION_JSON).content(BODY.replace("배차가 늦어요", " ")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("제목을 입력해 주세요."));
	}

	@Test
	void withoutLogin() throws Exception {
		mockMvc.perform(get("/api/inquiries/my"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private String token(Long memberId, MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(memberId).email("user@test.com").role(role).build());
	}
}
