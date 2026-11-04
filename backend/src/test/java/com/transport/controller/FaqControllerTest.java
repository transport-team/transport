package com.transport.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.transport.config.SecurityConfig;
import com.transport.dto.FaqDTO;
import com.transport.service.FaqService;
import com.transport.util.JWTUtil;

// FAQ 공개 API: 로그인 없이 목록 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = FaqController.class, properties = "app.jwt.secret=" + FaqControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class FaqControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private FaqService faqService;

	@Test
	void listWithoutLogin() throws Exception {
		when(faqService.getFaqs("결제")).thenReturn(List.of(
				FaqDTO.builder().id(1L).category("결제").question("결제하지 않으면 어떻게 되나요?").active(true).build()));

		mockMvc.perform(get("/api/faqs").param("category", "결제"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].category").value("결제"))
				.andExpect(jsonPath("$[0].question").value("결제하지 않으면 어떻게 되나요?"));
		verify(faqService).getFaqs("결제");
	}
}
