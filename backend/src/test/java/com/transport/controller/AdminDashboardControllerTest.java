package com.transport.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.transport.config.SecurityConfig;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.DailyStatDTO;
import com.transport.dto.DashboardDTO;
import com.transport.dto.DriverRankDTO;
import com.transport.dto.VehicleTypeStatDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.service.DashboardService;
import com.transport.util.JWTUtil;

// 관리자 대시보드 API: 관리자만, 기본 기간 14일, 날짜 형식 · 기간 검사 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = AdminDashboardController.class, properties = "app.jwt.secret=" + AdminDashboardControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class AdminDashboardControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private DashboardService dashboardService;

	@Test
	void dashboard() throws Exception {
		when(dashboardService.getDashboard(any(LocalDate.class))).thenReturn(DashboardDTO.builder()
				.baseDate(LocalDate.of(2026, 11, 6))
				.todayReceivedCount(3)
				.todayCompletedCount(4)
				.monthRevenue(4_200_000)
				.vehicleTypeStats(List.of(VehicleTypeStatDTO.builder().vehicleTypeId(2L).vehicleTypeName("1톤").count(40).revenue(4_000_000).build()))
				.driverRanks(List.of(DriverRankDTO.builder().rank(1).driverId(3L).driverName("김기사").completedCount(7).averageRating(4.8).build()))
				.averageRating(4.6)
				.reviewCount(12)
				.waitingInquiryCount(2)
				.build());

		LocalDate before = LocalDate.now();
		mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.baseDate").value("2026-11-06"))
				.andExpect(jsonPath("$.todayReceivedCount").value(3))
				.andExpect(jsonPath("$.todayCompletedCount").value(4))
				.andExpect(jsonPath("$.monthRevenue").value(4_200_000))
				.andExpect(jsonPath("$.vehicleTypeStats[0].vehicleTypeName").value("1톤"))
				.andExpect(jsonPath("$.driverRanks[0].rank").value(1))
				.andExpect(jsonPath("$.driverRanks[0].driverName").value("김기사"))
				.andExpect(jsonPath("$.driverRanks[0].completedCount").value(7))
				.andExpect(jsonPath("$.driverRanks[0].averageRating").value(4.8))
				.andExpect(jsonPath("$.averageRating").value(4.6))
				.andExpect(jsonPath("$.reviewCount").value(12))
				.andExpect(jsonPath("$.waitingInquiryCount").value(2));
		LocalDate after = LocalDate.now();

		// 기준일은 서버 오늘 날짜
		ArgumentCaptor<LocalDate> today = ArgumentCaptor.forClass(LocalDate.class);
		verify(dashboardService).getDashboard(today.capture());
		assertThat(today.getValue()).isBetween(before, after);
	}

	@Test
	void dailyWithRange() throws Exception {
		LocalDate from = LocalDate.of(2026, 11, 1);
		LocalDate to = LocalDate.of(2026, 11, 2);
		when(dashboardService.getDailyStats(from, to)).thenReturn(List.of(
				new DailyStatDTO(from, 0, 0), new DailyStatDTO(to, 2, 30_000)));

		mockMvc.perform(get("/api/admin/stats/daily").param("from", "2026-11-01").param("to", "2026-11-02")
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].date").value("2026-11-02"))
				.andExpect(jsonPath("$[1].count").value(2))
				.andExpect(jsonPath("$[1].revenue").value(30_000));
	}

	// 기간을 주지 않으면 오늘까지 14일
	@Test
	void dailyDefaultsToLast14Days() throws Exception {
		LocalDate before = LocalDate.now();
		mockMvc.perform(get("/api/admin/stats/daily").header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk());
		LocalDate after = LocalDate.now();

		ArgumentCaptor<LocalDate> from = ArgumentCaptor.forClass(LocalDate.class);
		ArgumentCaptor<LocalDate> to = ArgumentCaptor.forClass(LocalDate.class);
		verify(dashboardService).getDailyStats(from.capture(), to.capture());
		assertThat(to.getValue()).isBetween(before, after);
		assertThat(from.getValue()).isEqualTo(to.getValue().minusDays(13));
	}

	// to만 주면 그날까지 14일
	@Test
	void dailyWithOnlyTo() throws Exception {
		mockMvc.perform(get("/api/admin/stats/daily").param("to", "2026-11-06")
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isOk());

		verify(dashboardService).getDailyStats(LocalDate.of(2026, 10, 24), LocalDate.of(2026, 11, 6));
	}

	@Test
	void dailyRejectsWrongDateFormat() throws Exception {
		mockMvc.perform(get("/api/admin/stats/daily").param("from", "2026/11/01")
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
		verify(dashboardService, never()).getDailyStats(any(), any());
	}

	// 기간 검사(서비스)에서 막히면 400과 문구
	@Test
	void dailyRangeError() throws Exception {
		when(dashboardService.getDailyStats(any(), any()))
				.thenThrow(new CustomException(ErrorCode.INVALID_INPUT, "일별 통계는 최대 92일까지 볼 수 있습니다."));

		mockMvc.perform(get("/api/admin/stats/daily").param("from", "2026-01-01").param("to", "2026-11-06")
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.ADMIN)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value("일별 통계는 최대 92일까지 볼 수 있습니다."));
	}

	@ParameterizedTest
	@EnumSource(value = MemberRole.class, names = {"CUSTOMER", "DRIVER"})
	void onlyAdmin(MemberRole role) throws Exception {
		mockMvc.perform(get("/api/admin/dashboard").header(HttpHeaders.AUTHORIZATION, token(role)))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("FORBIDDEN"));
		mockMvc.perform(get("/api/admin/stats/daily").header(HttpHeaders.AUTHORIZATION, token(role)))
				.andExpect(status().isForbidden());
		verifyNoInteractions(dashboardService);
	}

	@Test
	void loginRequired() throws Exception {
		mockMvc.perform(get("/api/admin/dashboard"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("user@test.com").role(role).build());
	}
}
