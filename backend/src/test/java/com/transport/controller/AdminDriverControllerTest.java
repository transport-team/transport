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
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.DriverDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.DriverService;
import com.transport.util.JWTUtil;

// 관리자 기사 API: 관리자만 · 입력 검사 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = AdminDriverController.class, properties = "app.jwt.secret=" + AdminDriverControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class AdminDriverControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private DriverService driverService;

	@Test
	void listForAdmin() throws Exception {
		when(driverService.getDrivers(any(DriverSearchDTO.class))).thenReturn(PageResponseDTO.<DriverDTO>withAll()
				.dtoList(List.of(DriverDTO.builder().id(7L).name("김기사").status(DriverStatus.PENDING).build()))
				.pageRequestDTO(new PageRequestDTO())
				.totalCount(1)
				.build());

		mockMvc.perform(get("/api/admin/drivers").param("status", "PENDING").param("keyword", "김")
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.dtoList[0].name").value("김기사"))
				.andExpect(jsonPath("$.totalCount").value(1));
		verify(driverService).getDrivers(argThat(search ->
				search.getStatus() == DriverStatus.PENDING && "김".equals(search.getKeyword()) && search.getPage() == 1));
	}

	@Test
	void customerIsForbidden() throws Exception {
		mockMvc.perform(get("/api/admin/drivers").header(HttpHeaders.AUTHORIZATION, token(MemberRole.CUSTOMER)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
	}

	@Test
	void loginRequired() throws Exception {
		mockMvc.perform(get("/api/admin/drivers"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	void approve() throws Exception {
		when(driverService.changeStatus(eq(7L), eq(DriverStatus.ACTIVE)))
				.thenReturn(DriverDTO.builder().id(7L).status(DriverStatus.ACTIVE).build());

		mockMvc.perform(patch("/api/admin/drivers/7/status").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{\"status\": \"ACTIVE\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("ACTIVE"));
	}

	@Test
	void statusRequired() throws Exception {
		mockMvc.perform(patch("/api/admin/drivers/7/status").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN))
						.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("변경할 상태를 선택해 주세요."));
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("user@test.com").role(role).build());
	}
}
