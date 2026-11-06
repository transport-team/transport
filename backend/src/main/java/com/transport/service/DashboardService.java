package com.transport.service;

import java.time.LocalDate;
import java.util.List;

import com.transport.dto.DailyStatDTO;
import com.transport.dto.DashboardDTO;

// 관리자 대시보드 (팀원 1, ADM-01) — 다른 도메인 숫자는 담당자 Service 메서드로만 가져온다 (docs/collaboration.md 약속 표)
public interface DashboardService {

	// 일별 통계로 한 번에 볼 수 있는 최대 일수
	int MAX_DAILY_DAYS = 92;

	// 오늘 · 지금 · 이번 달 숫자, 처리할 일, 이번 달 차종별 (today: 기준일 — 컨트롤러가 서버 날짜를 넘긴다)
	DashboardDTO getDashboard(LocalDate today);

	// from ~ to(포함) 날짜별 결제 완료 건수 · 매출. 결제가 없는 날은 0, 최대 92일
	List<DailyStatDTO> getDailyStats(LocalDate from, LocalDate to);
}
