package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.LocationResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.LocationLogRepository;
import com.transport.repository.ShipmentRepository;

// 위치 전송(sendLocation) 테스트 (9-1 보강 — 팀원 4)
// 요구사항: 배정 기사만, IN_TRANSIT일 때만. 부정 케이스 4개 + 정상 1개.
// (조회 getLatestLocation 테스트는 LocationServiceImplTest에 있음)
@ExtendWith(MockitoExtension.class)
class LocationSendServiceImplTest {

	private static final Long SHIPMENT_ID = 1L;
	private static final Long DRIVER_MEMBER_ID = 200L;   // 배정 기사 (회원 ID)
	private static final Long DRIVER_ID = 20L;           // 배정 기사 (Driver 엔티티 ID)
	private static final Long OTHER_DRIVER_ID = 99L;     // 다른 기사 (Driver 엔티티 ID)

	@Mock
	LocationLogRepository locationLogRepository;

	@Mock
	ShipmentRepository shipmentRepository;

	@Mock
	DispatchRepository dispatchRepository;

	@Mock
	DriverRepository driverRepository;

	@Mock
	NotificationServiceImpl notificationService;

	@InjectMocks
	LocationServiceImpl locationService;

	private void givenDriver(Long driverId) {
		when(driverRepository.findByMemberId(DRIVER_MEMBER_ID))
				.thenReturn(Optional.of(Driver.builder().id(driverId).build()));
	}

	private void givenShipment(ShipmentStatus status) {
		Shipment shipment = Shipment.builder()
				.id(SHIPMENT_ID)
				.status(status)
				.customer(Member.builder().id(100L).build())
				.build();
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
	}

	private void givenActiveDriver(Long driverId) {
		when(dispatchRepository.findByShipmentIdAndActiveTrue(SHIPMENT_ID))
				.thenReturn(java.util.Optional.of(
						com.transport.domain.Dispatch.builder().driver(Driver.builder().id(driverId).build()).build()));
	}

	@Test
	@DisplayName("정상: 배정 기사가 운송중 배송에 위치를 보내면 저장된다")
	void sendLocation_success() {
		givenShipment(ShipmentStatus.IN_TRANSIT);
		givenDriver(DRIVER_ID);
		givenActiveDriver(DRIVER_ID);
		when(locationLogRepository.save(any(com.transport.domain.LocationLog.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		LocationResponseDTO result = locationService.sendLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, 37.5, 127.0);

		assertThat(result.getLat()).isEqualTo(37.5);
		assertThat(result.getLng()).isEqualTo(127.0);
		verify(locationLogRepository).save(any(com.transport.domain.LocationLog.class));
	}

	@Test
	@DisplayName("부정: 다른 기사의 위치 전송은 403 NOT_ASSIGNED_DRIVER")
	void sendLocation_wrongDriver() {
		givenShipment(ShipmentStatus.IN_TRANSIT);
		givenDriver(OTHER_DRIVER_ID);          // 로그인한 기사
		givenActiveDriver(DRIVER_ID);          // 실제 배정 기사는 다른 사람

		CustomException ex = assertThrows(CustomException.class,
				() -> locationService.sendLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, 37.5, 127.0));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER);
		verify(locationLogRepository, never()).save(any(com.transport.domain.LocationLog.class));
	}

	@Test
	@DisplayName("부정: 배차가 없으면 403 NOT_ASSIGNED_DRIVER")
	void sendLocation_noDispatch() {
		givenShipment(ShipmentStatus.IN_TRANSIT);
		givenDriver(DRIVER_ID);
		when(dispatchRepository.findByShipmentIdAndActiveTrue(SHIPMENT_ID))
				.thenReturn(java.util.Optional.empty());

		CustomException ex = assertThrows(CustomException.class,
				() -> locationService.sendLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, 37.5, 127.0));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER);
	}

	@Test
	@DisplayName("부정: 운송중이 아니면 배정 기사여도 400 NOT_IN_TRANSIT")
	void sendLocation_notInTransit() {
		givenShipment(ShipmentStatus.ARRIVED);   // 도착 후 — 더 이상 위치를 받지 않는다
		givenDriver(DRIVER_ID);
		givenActiveDriver(DRIVER_ID);

		CustomException ex = assertThrows(CustomException.class,
				() -> locationService.sendLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, 37.5, 127.0));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_IN_TRANSIT);
		verify(locationLogRepository, never()).save(any(com.transport.domain.LocationLog.class));
	}

	@Test
	@DisplayName("부정: 없는 배송은 404 SHIPMENT_NOT_FOUND")
	void sendLocation_shipmentNotFound() {
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(java.util.Optional.empty());

		CustomException ex = assertThrows(CustomException.class,
				() -> locationService.sendLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, 37.5, 127.0));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHIPMENT_NOT_FOUND);
	}
}