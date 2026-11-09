package com.transport.service;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentRepository;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;

// StatusServiceImpl 단위 테스트 (팀원 4 — 4-3)
@ExtendWith(MockitoExtension.class)
class StatusServiceTest {

	@Mock
	ShipmentRepository shipmentRepository;

	@Mock
	DispatchService dispatchService;

	@Mock
	StatusHistoryService statusHistoryService;

	@Mock
	NotificationService notificationService;

	@Mock
	Shipment shipment; // changeStatus/updateStatus 검증용 mock

	@InjectMocks
	StatusServiceImpl statusService;

	private void givenShipment(ShipmentStatus status, Long driverId) {
		lenient().doNothing().when(shipment).updateStatus(any(ShipmentStatus.class));
		lenient().when(shipment.getId()).thenReturn(1L);
		lenient().when(shipment.getStatus()).thenReturn(status);
		lenient().when(shipment.getCustomer()).thenReturn(Member.builder().id(100L).build());
		when(shipmentRepository.findById(1L)).thenReturn(Optional.of(shipment));
		lenient().when(dispatchService.getActiveDriver(1L)).thenReturn(driverId);
	}

	@Test
	@DisplayName("정상: 배정 기사가 상태 변경 성공")
	void changeStatus_success() {
		givenShipment(ShipmentStatus.ASSIGNED, 10L);

		statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "상차 완료", 10L);

		verify(shipment).updateStatus(ShipmentStatus.PICKED_UP);
		verify(statusHistoryService).record(eq(shipment), eq(ShipmentStatus.ASSIGNED),
				eq(ShipmentStatus.PICKED_UP), any(), anyString());
		verify(notificationService).send(any(), any(), anyString(), eq(shipment));
	}

	@Test
	@DisplayName("예외: 잘못된 전이 (REQUESTED -> COMPLETED)")
	void changeStatus_invalidTransition() {
		givenShipment(ShipmentStatus.REQUESTED, 10L);

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.changeStatus(1L, ShipmentStatus.COMPLETED, "", 10L),
				CustomException.class);

		org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_STATUS_CHANGE);

		verify(shipment, never()).updateStatus(any(ShipmentStatus.class));
		verify(statusHistoryService, never()).record(any(), any(), any(), any(), anyString());
	}

	@Test
	@DisplayName("예외: 다른 기사의 요청 (NOT_ASSIGNED_DRIVER)")
	void changeStatus_wrongDriver() {
		givenShipment(ShipmentStatus.ASSIGNED, 20L); // 배정 기사 = 20, 요청자 = 10

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "", 10L),
				CustomException.class);

		org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER);

		verify(shipment, never()).updateStatus(any(ShipmentStatus.class));
	}

	@Test
	@DisplayName("예외: 배정 기사 없음 (null 반환)")
	void changeStatus_noAssignedDriver() {
		givenShipment(ShipmentStatus.ASSIGNED, null);

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "", 10L),
				CustomException.class);

		org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER);

		verify(shipment, never()).updateStatus(any(ShipmentStatus.class));
	}

	@Test
	@DisplayName("예외: 존재하지 않는 배송")
	void changeStatus_shipmentNotFound() {
		when(shipmentRepository.findById(1L)).thenReturn(Optional.empty());

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "", 10L),
				CustomException.class);

		org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHIPMENT_NOT_FOUND);
	}
}