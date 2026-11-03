package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.transport.dto.SignupRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.service.MemberService;

// 회원가입 API: 응답 코드 · 입력 검사 (보안 필터는 끄고 확인)
@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private MemberService memberService;

	@Test
	void signup() throws Exception {
		when(memberService.signup(any(SignupRequestDTO.class))).thenReturn(4L);

		signup("kim@test.com", "password1!")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.memberId").value(4));
	}

	@Test
	void invalidEmail() throws Exception {
		signup("not-an-email", "password1!")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value("이메일 형식이 올바르지 않습니다."));
	}

	@Test
	void shortPassword() throws Exception {
		signup("kim@test.com", "1234")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("비밀번호는 8~64자로 입력해 주세요."));
	}

	@Test
	void duplicateEmail() throws Exception {
		when(memberService.signup(any(SignupRequestDTO.class))).thenThrow(new CustomException(ErrorCode.DUPLICATE_EMAIL));

		signup("kim@test.com", "password1!")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_EMAIL"));
	}

	private ResultActions signup(String email, String password) throws Exception {
		String body = """
				{"email": "%s", "password": "%s", "name": "김고객", "phone": "010-1234-5678",
				 "role": "CUSTOMER", "agreedTermIds": [1, 2]}
				""".formatted(email, password);
		return mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body));
	}
}
