package com.transport.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 관리자 대시보드 1단계 (ADM-01, GET /api/admin/dashboard) — 숫자를 세는 기준은 docs/decisions.md
// 오늘 완료 · 평균 평점 · 기사 순위는 담당 기능이 생기면 2단계로 더한다
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDTO {

	private LocalDate baseDate;  // 기준일 (서버 날짜)

	// 오늘: 결제 완료 = 접수 (status.md REQUESTED)
	private long todayReceivedCount;  // 오늘 결제가 끝난 건수
	private long todayRevenue;        // 오늘 결제 금액 합계

	// 지금: 배송 상태
	private long waitingDispatchCount;  // 배차 대기 (REQUESTED)
	private long inProgressCount;       // 진행 중 (ASSIGNED · PICKED_UP · IN_TRANSIT · ARRIVED)

	// 이번 달: 1일 0시 ~ 지금
	private long monthPaidCount;
	private long monthRevenue;
	private List<VehicleTypeStatDTO> vehicleTypeStats;  // 차종별 (사용 중인 차종은 결제가 없어도 0으로)

	// 처리할 일
	private long pendingDriverCount;          // 기사 승인 대기
	private long pendingVehicleRequestCount;  // 차량 등록 신청 대기
	private long waitingInquiryCount;         // 답변 대기 문의
}
