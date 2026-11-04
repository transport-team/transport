package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.web.servlet.ResultActions;

import com.transport.config.SecurityConfig;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.MemberDTO;
import com.transport.dto.MemberModifyDTO;
import com.transport.dto.PasswordChangeDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.service.MemberService;
import com.transport.util.JWTUtil;

// 토큰 확인 필터 · 401 응답, 내 정보 수정 · 비밀번호 변경 (보안 필터를 켜고 내 정보 API로 확인)
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

	@Test
	void modifyPhone() throws Exception {
		when(memberService.modify(eq(4L), any(MemberModifyDTO.class))).thenReturn(MemberDTO.builder()
				.id(4L).email("kim@test.com").name("김고객").phone("010-9999-8888").role(MemberRole.CUSTOMER).build());

		mockMvc.perform(put("/api/members/me").header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON).content("{\"phone\": \"010-9999-8888\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.phone").value("010-9999-8888"));
	}

	@Test
	void modifyWithWrongPhoneFormat() throws Exception {
		mockMvc.perform(put("/api/members/me").header(HttpHeaders.AUTHORIZATION, bearer())
						.contentType(MediaType.APPLICATION_JSON).content("{\"phone\": \"02-123-4567\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value("연락처는 010-0000-0000 형식으로 입력해 주세요."));
	}

	@Test
	void modifyWithoutToken() throws Exception {
		mockMvc.perform(put("/api/members/me")
						.contentType(MediaType.APPLICATION_JSON).content("{\"phone\": \"010-9999-8888\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void changePassword() throws Exception {
		changePassword("{\"currentPassword\": \"password1!\", \"newPassword\": \"newPassword1!\"}")
				.andExpect(status().isNoContent());
		verify(memberService).changePassword(eq(4L), any(PasswordChangeDTO.class));
	}

	@Test
	void wrongCurrentPassword() throws Exception {
		doThrow(new CustomException(ErrorCode.WRONG_PASSWORD))
				.when(memberService).changePassword(eq(4L), any(PasswordChangeDTO.class));

		changePassword("{\"currentPassword\": \"wrong-password\", \"newPassword\": \"newPassword1!\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
	}

	@Test
	void newPasswordTooShort() throws Exception {
		changePassword("{\"currentPassword\": \"password1!\", \"newPassword\": \"short\"}")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("새 비밀번호는 8~64자로 입력해 주세요."));
	}

	private ResultActions me(String authorization) throws Exception {
		return mockMvc.perform(get("/api/members/me").header(HttpHeaders.AUTHORIZATION, authorization));
	}

	private ResultActions changePassword(String body) throws Exception {
		return mockMvc.perform(patch("/api/members/me/password").header(HttpHeaders.AUTHORIZATION, bearer())
				.contentType(MediaType.APPLICATION_JSON).content(body));
	}

	private String bearer() {
		return "Bearer " + jwtUtil.createAccessToken(member);
	}
}
