package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.transport.config.SecurityConfig;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.NotificationService;
import com.transport.util.JWTUtil;

// 알림 목록 API (6-2 후속): 페이지 값이 틀리면 500이 아니라 400 (다른 목록 API와 같은 검사, 보안 필터를 켜고 확인)
@WebMvcTest(controllers = NotificationController.class, properties = "app.jwt.secret=" + NotificationControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class NotificationControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private NotificationService notificationService;

	@Test
	void defaultPageIsOneAndTen() throws Exception {
		when(notificationService.getMyNotifications(eq(1L), any(PageRequestDTO.class)))
				.thenReturn(new PageResponseDTO<>(List.of(), PageRequestDTO.builder().page(1).size(10).build(), 0));
		when(notificationService.getUnreadCount(1L)).thenReturn(0L);

		mockMvc.perform(get("/api/notifications").header(HttpHeaders.AUTHORIZATION, token()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.unreadCount").value(0));
		verify(notificationService).getMyNotifications(eq(1L), argThat(p -> p.getPage() == 1 && p.getSize() == 10));
	}

	@Test
	void wrongPageValuesAreBadRequest() throws Exception {
		for (String query : List.of("page=0", "page=-1", "size=0", "size=101")) {
			mockMvc.perform(get("/api/notifications?" + query).header(HttpHeaders.AUTHORIZATION, token()))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		}
		verifyNoInteractions(notificationService);
	}

	private String token() {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("customer@test.com").role(MemberRole.CUSTOMER).build());
	}
}
