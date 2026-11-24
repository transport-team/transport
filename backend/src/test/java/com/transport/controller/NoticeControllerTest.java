package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
import com.transport.dto.NoticeDTO;
import com.transport.dto.NoticeSearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.NoticeService;
import com.transport.util.JWTUtil;

// 공지 공개 API: 로그인 없이 목록 · 상세 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = NoticeController.class, properties = "app.jwt.secret=" + NoticeControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class NoticeControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private NoticeService noticeService;

	@Test
	void listWithoutLogin() throws Exception {
		when(noticeService.getNotices(any(NoticeSearchDTO.class))).thenReturn(PageResponseDTO.<NoticeDTO>withAll()
				.dtoList(List.of(NoticeDTO.builder().id(1L).title("서비스 오픈 안내").writerName("관리자").build()))
				.pageRequestDTO(new PageRequestDTO())
				.totalCount(1)
				.build());

		mockMvc.perform(get("/api/notices").param("keyword", "오픈"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dtoList[0].title").value("서비스 오픈 안내"))
				.andExpect(jsonPath("$.totalCount").value(1));
		verify(noticeService).getNotices(argThat(search -> "오픈".equals(search.getKeyword()) && search.getPage() == 1));
	}

	@Test
	void readWithoutLogin() throws Exception {
		when(noticeService.getNotice(1L)).thenReturn(NoticeDTO.builder().id(1L).title("서비스 오픈 안내").viewCount(4).build());

		mockMvc.perform(get("/api/notices/1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.viewCount").value(4));
	}
}
