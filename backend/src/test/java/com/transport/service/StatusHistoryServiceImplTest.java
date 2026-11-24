package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverCompletedStatDTO;
import com.transport.repository.ShipmentStatusHistoryRepository;

// StatusHistoryServiceImpl 단위 테스트
@ExtendWith(MockitoExtension.class)
class StatusHistoryServiceImplTest {

	@Mock
	ShipmentStatusHistoryRepository historyRepository;

	@InjectMocks
	StatusHistoryServiceImpl statusHistoryService;

	@Test
	@DisplayName("상태 변경 이력 저장 - 정상")
	void record_savesHistory() {
		// given
		Shipment shipment = Shipment.builder().id(1L).build();
		Member changedBy = Member.builder().id(10L).build();
		ShipmentStatus from = ShipmentStatus.ASSIGNED;
		ShipmentStatus to = ShipmentStatus.PICKED_UP;
		String memo = "상차 완료";

		ShipmentStatusHistory savedHistory = ShipmentStatusHistory.builder()
				.id(100L)
				.shipment(shipment)
				.fromStatus(from)
				.toStatus(to)
				.changedBy(changedBy)
				.memo(memo)
				.changedAt(LocalDateTime.now())
				.build();

		when(historyRepository.save(any(ShipmentStatusHistory.class))).thenReturn(savedHistory);

		// when
		ShipmentStatusHistory result = statusHistoryService.record(shipment, from, to, changedBy, memo);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(100L);
		assertThat(result.getShipment()).isEqualTo(shipment);
		assertThat(result.getFromStatus()).isEqualTo(from);
		assertThat(result.getToStatus()).isEqualTo(to);
		assertThat(result.getChangedBy()).isEqualTo(changedBy);
		assertThat(result.getMemo()).isEqualTo(memo);
		assertThat(result.getChangedAt()).isNotNull();

		verify(historyRepository).save(any(ShipmentStatusHistory.class));
	}

	@Test
	@DisplayName("상태 변경 이력 저장 - 변경자 없음(시스템)")
	void record_savesHistoryWithNullChangedBy() {
		// given
		Shipment shipment = Shipment.builder().id(2L).build();
		ShipmentStatus from = null; // 최초 신청
		ShipmentStatus to = ShipmentStatus.PENDING_PAYMENT;
		String memo = "신청 접수";

		ShipmentStatusHistory savedHistory = ShipmentStatusHistory.builder()
				.id(200L)
				.shipment(shipment)
				.fromStatus(from)
				.toStatus(to)
				.changedBy(null)
				.memo(memo)
				.changedAt(LocalDateTime.now())
				.build();

		when(historyRepository.save(any(ShipmentStatusHistory.class))).thenReturn(savedHistory);

		// when
		ShipmentStatusHistory result = statusHistoryService.record(shipment, from, to, null, memo);

		// then
		assertThat(result).isNotNull();
		assertThat(result.getChangedBy()).isNull();
		assertThat(result.getFromStatus()).isNull();

		verify(historyRepository).save(any(ShipmentStatusHistory.class));
	}

	// ===== 관리자 대시보드 집계 (쿼리 자체는 StatusHistoryReviewStatsTest가 실제 DB로 확인) =====

	@Test
	@DisplayName("오늘 완료 - 배송완료(COMPLETED)로 바뀐 이력을 기간으로 센다")
	void countCompleted_countsCompletedInRange() {
		LocalDateTime from = LocalDateTime.of(2026, 11, 13, 0, 0);
		LocalDateTime to = from.plusDays(1);
		when(historyRepository.countByToStatusBetween(ShipmentStatus.COMPLETED, from, to)).thenReturn(4L);

		assertThat(statusHistoryService.countCompleted(from, to)).isEqualTo(4);
	}

	@Test
	@DisplayName("기사 순위 - 배송완료 이력을 기사별로 limit명까지, 결과를 DTO로 옮긴다")
	void getCompletedCountsByDriver_mapsRows() {
		LocalDateTime from = LocalDateTime.of(2026, 11, 1, 0, 0);
		LocalDateTime to = LocalDateTime.of(2026, 11, 14, 0, 0);
		when(historyRepository.countGroupByDriver(ShipmentStatus.COMPLETED, from, to, PageRequest.of(0, 5)))
				.thenReturn(List.of(driverCount(3L, "김기사", 7L), driverCount(5L, "이기사", 2L)));

		List<DriverCompletedStatDTO> result = statusHistoryService.getCompletedCountsByDriver(from, to, 5);

		assertThat(result)
				.extracting(DriverCompletedStatDTO::getDriverId, DriverCompletedStatDTO::getDriverName, DriverCompletedStatDTO::getCompletedCount)
				.containsExactly(tuple(3L, "김기사", 7L), tuple(5L, "이기사", 2L));
	}

	private ShipmentStatusHistoryRepository.DriverChangedCount driverCount(Long driverId, String name, Long count) {
		return new ShipmentStatusHistoryRepository.DriverChangedCount() {
			@Override
			public Long getDriverId() {
				return driverId;
			}

			@Override
			public String getDriverName() {
				return name;
			}

			@Override
			public Long getChangedCount() {
				return count;
			}
		};
	}
}