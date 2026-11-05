package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;
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
}