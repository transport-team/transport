package com.transport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DailyStatDTO;
import com.transport.dto.DashboardDTO;
import com.transport.dto.DriverCompletedStatDTO;
import com.transport.dto.DriverRankDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.PaidStatDTO;
import com.transport.dto.RatingStatDTO;
import com.transport.dto.VehicleTypeResponseDTO;
import com.transport.dto.VehicleTypeStatDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import lombok.RequiredArgsConstructor;

// DashboardService 구현 — 숫자를 세는 기준은 docs/decisions.md
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

	// 진행 중: 배차된 뒤 도착까지 (결제대기 · 배차 대기 · 완료 · 취소는 빠진다)
	private static final Set<ShipmentStatus> IN_PROGRESS =
			EnumSet.of(ShipmentStatus.ASSIGNED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.ARRIVED);

	// 기사 순위에 보여 줄 기사 수
	static final int DRIVER_RANK_SIZE = 5;

	private final ShipmentService shipmentService;                        // 팀원 3
	private final PaymentService paymentService;                          // 팀원 3
	private final VehicleTypeService vehicleTypeService;                  // 팀원 2
	private final DriverService driverService;                            // 팀원 2
	private final VehicleRegistrationService vehicleRegistrationService;  // 팀원 2
	private final StatusHistoryService statusHistoryService;              // 팀원 4
	private final ReviewService reviewService;                            // 팀원 4
	private final InquiryService inquiryService;

	@Override
	public DashboardDTO getDashboard(LocalDate today) {
		LocalDateTime todayStart = today.atStartOfDay();
		LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime tomorrowStart = today.plusDays(1).atStartOfDay();

		PaidStatDTO todayStat = paymentService.getPaidStat(todayStart, tomorrowStart);
		PaidStatDTO monthStat = paymentService.getPaidStat(monthStart, tomorrowStart);
		Map<ShipmentStatus, Long> statusCounts = shipmentService.countByStatus();
		RatingStatDTO ratingStat = reviewService.getRatingStat();

		return DashboardDTO.builder()
				.baseDate(today)
				.todayReceivedCount(todayStat.getCount())
				.todayRevenue(todayStat.getRevenue())
				.todayCompletedCount(statusHistoryService.countCompleted(todayStart, tomorrowStart))
				.waitingDispatchCount(statusCounts.getOrDefault(ShipmentStatus.REQUESTED, 0L))
				.inProgressCount(IN_PROGRESS.stream().mapToLong(status -> statusCounts.getOrDefault(status, 0L)).sum())
				.monthPaidCount(monthStat.getCount())
				.monthRevenue(monthStat.getRevenue())
				.vehicleTypeStats(getVehicleTypeStats(monthStart, tomorrowStart))
				.driverRanks(getDriverRanks(monthStart, tomorrowStart))
				.averageRating(ratingStat.getAverageRating())
				.reviewCount(ratingStat.getReviewCount())
				.pendingDriverCount(countPendingDrivers())
				.pendingVehicleRequestCount(vehicleRegistrationService.getPendingRequests().size())
				.waitingInquiryCount(inquiryService.countWaiting())
				.build();
	}

	@Override
	public List<DailyStatDTO> getDailyStats(LocalDate from, LocalDate to) {
		if (from.isAfter(to)) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "시작일은 종료일보다 늦을 수 없습니다.");
		}
		if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAILY_DAYS) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "일별 통계는 최대 " + MAX_DAILY_DAYS + "일까지 볼 수 있습니다.");
		}

		Map<LocalDate, DailyStatDTO> paidByDate = paymentService.getDailyPaidStats(from, to).stream()
				.collect(Collectors.toMap(DailyStatDTO::getDate, Function.identity()));
		// 결제가 없는 날도 0으로 채워 차트가 끊기지 않게 한다
		return from.datesUntil(to.plusDays(1))
				.map(date -> paidByDate.getOrDefault(date, DailyStatDTO.builder().date(date).build()))
				.toList();
	}

	// 차종별: 사용 중인 차종은 결제가 없어도 0으로, 사용 중지된 차종은 결제가 있을 때만 (차종 id 순)
	private List<VehicleTypeStatDTO> getVehicleTypeStats(LocalDateTime from, LocalDateTime to) {
		Map<Long, VehicleTypeStatDTO> paidByType = paymentService.getPaidStatsByVehicleType(from, to).stream()
				.collect(Collectors.toMap(VehicleTypeStatDTO::getVehicleTypeId, Function.identity()));
		return vehicleTypeService.getVehicleTypes().stream()
				.filter(type -> type.isActive() || paidByType.containsKey(type.getId()))
				.sorted(Comparator.comparing(VehicleTypeResponseDTO::getId))
				.map(type -> paidByType.getOrDefault(type.getId(), VehicleTypeStatDTO.builder()
						.vehicleTypeId(type.getId())
						.vehicleTypeName(type.getName())
						.build()))
				.toList();
	}

	// 기사 순위: 기간 안 배송완료 건수 많은 순 5명(같으면 기사 id 순으로 자름), 건수가 같으면 같은 순위
	// 평점은 약속 메서드 ReviewService.getAverageRating(driverId) — 많아야 5번 부른다
	private List<DriverRankDTO> getDriverRanks(LocalDateTime from, LocalDateTime to) {
		List<DriverCompletedStatDTO> stats = statusHistoryService.getCompletedCountsByDriver(from, to, DRIVER_RANK_SIZE);
		List<DriverRankDTO> ranks = new ArrayList<>();
		for (int i = 0; i < stats.size(); i++) {
			DriverCompletedStatDTO stat = stats.get(i);
			boolean sameAsAbove = i > 0 && stat.getCompletedCount() == stats.get(i - 1).getCompletedCount();
			ranks.add(DriverRankDTO.builder()
					.rank(sameAsAbove ? ranks.get(i - 1).getRank() : i + 1)
					.driverId(stat.getDriverId())
					.driverName(stat.getDriverName())
					.completedCount(stat.getCompletedCount())
					.averageRating(reviewService.getAverageRating(stat.getDriverId()))
					.build());
		}
		return ranks;
	}

	// 기사 승인 대기: 관리자 기사 목록(팀원 2)의 전체 건수만 쓴다
	private long countPendingDrivers() {
		return driverService.getDrivers(DriverSearchDTO.builder().status(DriverStatus.PENDING).size(1).build())
				.getTotalCount();
	}
}
