package com.transport.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.DailyStatDTO;
import com.transport.dto.DashboardDTO;
import com.transport.service.DashboardService;

import lombok.RequiredArgsConstructor;

// 관리자 대시보드 API (ADM-01 1단계): 오늘 · 지금 · 이번 달 숫자와 처리할 일, 일별 결제 완료 건수 · 매출
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

	private static final int DEFAULT_DAILY_DAYS = 14;  // 기간을 주지 않으면 to까지 14일

	private final DashboardService dashboardService;

	@GetMapping("/dashboard")
	public DashboardDTO dashboard() {
		return dashboardService.getDashboard(LocalDate.now());
	}

	// ?from=2026-10-24&to=2026-11-06 (to가 없으면 오늘, from이 없으면 to까지 14일)
	@GetMapping("/stats/daily")
	public List<DailyStatDTO> daily(
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
		LocalDate end = (to != null) ? to : LocalDate.now();
		LocalDate start = (from != null) ? from : end.minusDays(DEFAULT_DAILY_DAYS - 1);
		return dashboardService.getDailyStats(start, end);
	}
}
