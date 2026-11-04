package com.transport.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.transport.dto.TermDTO;
import com.transport.service.TermService;

// 약관 목록 API: 응답 모양 (required 필드 이름 포함)
@WebMvcTest(TermController.class)
@AutoConfigureMockMvc(addFilters = false)
class TermControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TermService termService;

	@Test
	void list() throws Exception {
		when(termService.getActiveTerms()).thenReturn(List.of(
				TermDTO.builder().id(1L).title("서비스 이용약관").content("내용").required(true).version("v1.0").build(),
				TermDTO.builder().id(3L).title("마케팅 정보 수신 동의").content("내용").required(false).version("v1.0").build()));

		mockMvc.perform(get("/api/terms"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[0].title").value("서비스 이용약관"))
				.andExpect(jsonPath("$[0].required").value(true))
				.andExpect(jsonPath("$[1].required").value(false));
	}
}
