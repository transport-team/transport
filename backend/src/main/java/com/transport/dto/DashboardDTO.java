package com.transport.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 관리자 대시보드 (ADM-01, GET /api/admin/dashboard) — 숫자를 세는 기준은 docs/decisions.md
// 1단계(11-06): 오늘 접수 · 지금 · 이번 달 · 처리할 일, 2단계(11-13): 오늘 완료 · 평균 평점 · 기사 순위
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDTO {

	private LocalDate baseDate;  // 기준일 (서버 날짜)

	// 오늘: 결제 완료 = 접수 (status.md REQUESTED)
	private long todayReceivedCount;   // 오늘 결제가 끝난 건수
	private long todayRevenue;         // 오늘 결제 금액 합계
	private long todayCompletedCount;  // 오늘 배송완료(COMPLETED)로 바뀐 건수 (상태 이력 기준)

	// 지금: 배송 상태
	private long waitingDispatchCount;  // 배차 대기 (REQUESTED)
	private long inProgressCount;       // 진행 중 (ASSIGNED · PICKED_UP · IN_TRANSIT · ARRIVED)

	// 이번 달: 1일 0시 ~ 지금
	private long monthPaidCount;
	private long monthRevenue;
	private List<VehicleTypeStatDTO> vehicleTypeStats;  // 차종별 (사용 중인 차종은 결제가 없어도 0으로)
	private List<DriverRankDTO> driverRanks;            // 기사 순위: 배송완료 건수 많은 순 5명 (없으면 빈 목록)

	// 평점: 전체 기간 리뷰
	private double averageRating;  // 소수 첫째 자리, 리뷰가 없으면 0.0
	private long reviewCount;

	// 처리할 일
	private long pendingDriverCount;          // 기사 승인 대기
	private long pendingVehicleRequestCount;  // 차량 등록 신청 대기
	private long waitingInquiryCount;         // 답변 대기 문의
}
