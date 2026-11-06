package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DailyStatDTO;
import com.transport.dto.DashboardDTO;
import com.transport.dto.DriverDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.PaidStatDTO;
import com.transport.dto.VehicleRegistrationResponseDTO;
import com.transport.dto.VehicleTypeResponseDTO;
import com.transport.dto.VehicleTypeStatDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

// 관리자 대시보드: 다른 도메인 Service가 준 숫자를 기준대로 묶는지 (Service는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

	@Mock
	private ShipmentService shipmentService;

	@Mock
	private PaymentService paymentService;

	@Mock
	private VehicleTypeService vehicleTypeService;

	@Mock
	private DriverService driverService;

	@Mock
	private VehicleRegistrationService vehicleRegistrationService;

	@Mock
	private InquiryService inquiryService;

	@InjectMocks
	private DashboardServiceImpl dashboardService;

	@Test
	void dashboard() {
		LocalDateTime tomorrowStart = LocalDate.of(2026, 11, 7).atStartOfDay();
		LocalDateTime monthStart = LocalDate.of(2026, 11, 1).atStartOfDay();
		when(paymentService.getPaidStat(TODAY.atStartOfDay(), tomorrowStart)).thenReturn(new PaidStatDTO(3, 150_000));
		when(paymentService.getPaidStat(monthStart, tomorrowStart)).thenReturn(new PaidStatDTO(42, 4_200_000));
		when(shipmentService.countByStatus()).thenReturn(statusCounts());
		when(paymentService.getPaidStatsByVehicleType(monthStart, tomorrowStart)).thenReturn(List.of(
				VehicleTypeStatDTO.builder().vehicleTypeId(2L).vehicleTypeName("1톤").count(40).revenue(4_000_000).build(),
				VehicleTypeStatDTO.builder().vehicleTypeId(4L).vehicleTypeName("구형 2.5톤").count(2).revenue(200_000).build()));
		when(vehicleTypeService.getVehicleTypes()).thenReturn(List.of(
				vehicleType(2L, "1톤", true), vehicleType(1L, "소형", true),
				vehicleType(3L, "5톤", false), vehicleType(4L, "구형 2.5톤", false)));
		when(driverService.getDrivers(any(DriverSearchDTO.class))).thenReturn(PageResponseDTO.<DriverDTO>withAll()
				.dtoList(List.of())
				.pageRequestDTO(PageRequestDTO.builder().build())
				.totalCount(5)
				.build());
		when(vehicleRegistrationService.getPendingRequests()).thenReturn(List.of(
				VehicleRegistrationResponseDTO.builder().id(1L).build(), VehicleRegistrationResponseDTO.builder().id(2L).build()));
		when(inquiryService.countWaiting()).thenReturn(3L);

		DashboardDTO dashboard = dashboardService.getDashboard(TODAY);

		assertThat(dashboard.getBaseDate()).isEqualTo(TODAY);
		assertThat(dashboard.getTodayReceivedCount()).isEqualTo(3);
		assertThat(dashboard.getTodayRevenue()).isEqualTo(150_000);
		assertThat(dashboard.getWaitingDispatchCount()).isEqualTo(2);  // REQUESTED
		assertThat(dashboard.getInProgressCount()).isEqualTo(5);       // ASSIGNED 1 + PICKED_UP 1 + IN_TRANSIT 2 + ARRIVED 1
		assertThat(dashboard.getMonthPaidCount()).isEqualTo(42);
		assertThat(dashboard.getMonthRevenue()).isEqualTo(4_200_000);
		assertThat(dashboard.getPendingDriverCount()).isEqualTo(5);
		assertThat(dashboard.getPendingVehicleRequestCount()).isEqualTo(2);
		assertThat(dashboard.getWaitingInquiryCount()).isEqualTo(3);
		verify(driverService).getDrivers(argThat(search -> search.getStatus() == DriverStatus.PENDING));
	}

	// 차종별: 사용 중인 차종은 0으로라도, 사용 중지된 차종은 결제가 있을 때만, 차종 id 순
	@Test
	void vehicleTypeStats() {
		when(paymentService.getPaidStat(any(), any())).thenReturn(new PaidStatDTO());
		when(shipmentService.countByStatus()).thenReturn(statusCounts());
		when(paymentService.getPaidStatsByVehicleType(any(), any())).thenReturn(List.of(
				VehicleTypeStatDTO.builder().vehicleTypeId(2L).vehicleTypeName("1톤").count(40).revenue(4_000_000).build(),
				VehicleTypeStatDTO.builder().vehicleTypeId(4L).vehicleTypeName("구형 2.5톤").count(2).revenue(200_000).build()));
		when(vehicleTypeService.getVehicleTypes()).thenReturn(List.of(
				vehicleType(2L, "1톤", true), vehicleType(1L, "소형", true),
				vehicleType(3L, "5톤", false), vehicleType(4L, "구형 2.5톤", false)));
		when(driverService.getDrivers(any(DriverSearchDTO.class))).thenReturn(PageResponseDTO.<DriverDTO>withAll()
				.dtoList(List.of()).pageRequestDTO(PageRequestDTO.builder().build()).totalCount(0).build());

		DashboardDTO dashboard = dashboardService.getDashboard(TODAY);

		assertThat(dashboard.getVehicleTypeStats())
				.extracting(VehicleTypeStatDTO::getVehicleTypeId, VehicleTypeStatDTO::getVehicleTypeName,
						VehicleTypeStatDTO::getCount, VehicleTypeStatDTO::getRevenue)
				.containsExactly(tuple(1L, "소형", 0L, 0L), tuple(2L, "1톤", 40L, 4_000_000L), tuple(4L, "구형 2.5톤", 2L, 200_000L));
	}

	// 매달 1일에는 오늘과 이번 달이 같은 기간
	@Test
	void firstDayOfMonth() {
		LocalDate firstDay = LocalDate.of(2026, 12, 1);
		when(paymentService.getPaidStat(firstDay.atStartOfDay(), firstDay.plusDays(1).atStartOfDay())).thenReturn(new PaidStatDTO(1, 50_000));
		when(shipmentService.countByStatus()).thenReturn(statusCounts());
		when(driverService.getDrivers(any(DriverSearchDTO.class))).thenReturn(PageResponseDTO.<DriverDTO>withAll()
				.dtoList(List.of()).pageRequestDTO(PageRequestDTO.builder().build()).totalCount(0).build());

		DashboardDTO dashboard = dashboardService.getDashboard(firstDay);

		assertThat(dashboard.getTodayRevenue()).isEqualTo(50_000);
		assertThat(dashboard.getMonthRevenue()).isEqualTo(50_000);
	}

	// 결제가 없는 날도 0으로 채운다 (from · to 포함)
	@Test
	void dailyStatsFillEmptyDays() {
		LocalDate from = LocalDate.of(2026, 11, 1);
		LocalDate to = LocalDate.of(2026, 11, 3);
		when(paymentService.getDailyPaidStats(from, to)).thenReturn(List.of(new DailyStatDTO(LocalDate.of(2026, 11, 2), 2, 30_000)));

		List<DailyStatDTO> daily = dashboardService.getDailyStats(from, to);

		assertThat(daily).extracting(DailyStatDTO::getDate, DailyStatDTO::getCount, DailyStatDTO::getRevenue)
				.containsExactly(tuple(from, 0L, 0L), tuple(LocalDate.of(2026, 11, 2), 2L, 30_000L), tuple(to, 0L, 0L));
	}

	@Test
	void dailyStatsUpTo92Days() {
		LocalDate from = TODAY.minusDays(91);
		when(paymentService.getDailyPaidStats(from, TODAY)).thenReturn(List.of());

		assertThat(dashboardService.getDailyStats(from, TODAY)).hasSize(92);
		assertThat(dashboardService.getDailyStats(TODAY, TODAY)).hasSize(1);
	}

	@Test
	void dailyStatsRejectLongRange() {
		assertThatThrownBy(() -> dashboardService.getDailyStats(TODAY.minusDays(92), TODAY))
				.isInstanceOf(CustomException.class)
				.hasMessage("일별 통계는 최대 92일까지 볼 수 있습니다.")
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		verify(paymentService, never()).getDailyPaidStats(any(), any());
	}

	@Test
	void dailyStatsRejectReversedRange() {
		assertThatThrownBy(() -> dashboardService.getDailyStats(TODAY, TODAY.minusDays(1)))
				.isInstanceOf(CustomException.class)
				.hasMessage("시작일은 종료일보다 늦을 수 없습니다.")
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		verify(paymentService, never()).getDailyPaidStats(any(), any());
	}

	private Map<ShipmentStatus, Long> statusCounts() {
		Map<ShipmentStatus, Long> counts = new EnumMap<>(ShipmentStatus.class);
		counts.put(ShipmentStatus.PENDING_PAYMENT, 4L);
		counts.put(ShipmentStatus.REQUESTED, 2L);
		counts.put(ShipmentStatus.ASSIGNED, 1L);
		counts.put(ShipmentStatus.PICKED_UP, 1L);
		counts.put(ShipmentStatus.IN_TRANSIT, 2L);
		counts.put(ShipmentStatus.ARRIVED, 1L);
		counts.put(ShipmentStatus.COMPLETED, 9L);
		counts.put(ShipmentStatus.CANCELED, 3L);
		return counts;
	}

	private VehicleTypeResponseDTO vehicleType(Long id, String name, boolean active) {
		return VehicleTypeResponseDTO.builder().id(id).name(name).active(active).build();
	}
}
