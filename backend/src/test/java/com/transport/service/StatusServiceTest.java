package com.transport.service;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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

	@InjectMocks
	StatusServiceImpl statusService;

	@Test
	@DisplayName("정상: 배정 기사가 상태 변경 성공")
	void changeStatus_success() {
		// given
		Long shipmentId = 1L;
		Long driverId = 10L;
		Shipment shipment = Shipment.builder()
				.id(shipmentId)
				.status(ShipmentStatus.ASSIGNED)
				.customer(Member.builder().id(100L).build())
				.build();
	
		when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
		when(dispatchService.getActiveDriver(shipmentId)).thenReturn(driverId);
	
		// when
		statusService.changeStatus(shipmentId, ShipmentStatus.PICKED_UP, "상차 완료", driverId);
	
		// then
		verify(shipment).changeStatus(ShipmentStatus.PICKED_UP);
		verify(statusHistoryService).record(eq(shipment), eq(ShipmentStatus.ASSIGNED), eq(ShipmentStatus.PICKED_UP), any(), anyString());
		verify(notificationService).send(any(), any(), anyString(), eq(shipment));
	}

	@Test
	@DisplayName("예외: 잘못된 전이 (REQUESTED -> COMPLETED)")
	void changeStatus_invalidTransition() {
		// given
		Long shipmentId = 1L;
		Long driverId = 10L;
		Shipment shipment = Shipment.builder()
				.id(shipmentId)
				.status(ShipmentStatus.REQUESTED)
				.customer(Member.builder().id(100L).build())
				.build();
	
		when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
		when(dispatchService.getActiveDriver(shipmentId)).thenReturn(driverId);
	
		// when & then
		assertThatThrownBy(() -> statusService.changeStatus(shipmentId, ShipmentStatus.COMPLETED, "", driverId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_STATUS_CHANGE);
	
		verify(shipment, never()).changeStatus(any());
	}

	@Test
	@DisplayName("예외: 다른 기사의 요청 (403 NOT_ASSIGNED_DRIVER)")
	void changeStatus_wrongDriver() {
		// given
		Long shipmentId = 1L;
		Long driverId = 10L;
		Long otherDriverId = 20L;
		Shipment shipment = Shipment.builder()
				.id(shipmentId)
				.status(ShipmentStatus.ASSIGNED)
				.customer(Member.builder().id(100L).build())
				.build();
	
		when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
		when(dispatchService.getActiveDriver(shipmentId)).thenReturn(otherDriverId);
	
		// when & then
		assertThatThrownBy(() -> statusService.changeStatus(shipmentId, ShipmentStatus.PICKED_UP, "", driverId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ASSIGNED_DRIVER);
	}

	@Test
	@DisplayName("예외: 배정 기사 없음 (NULL 반환)")
	void changeStatus_noAssignedDriver() {
		// given
		Long shipmentId = 1L;
		Long driverId = 10L;
		Shipment shipment = Shipment.builder()
				.id(shipmentId)
				.status(ShipmentStatus.ASSIGNED)
				.customer(Member.builder().id(100L).build())
				.build();
	
		when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
		when(dispatchService.getActiveDriver(shipmentId)).thenReturn(null);
	
		// when & then
		assertThatThrownBy(() -> statusService.changeStatus(shipmentId, ShipmentStatus.PICKED_UP, "", driverId))
				.isInstanceOf(CustomException.class)
				.hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ASSIGNED_DRIVER);
	}
}