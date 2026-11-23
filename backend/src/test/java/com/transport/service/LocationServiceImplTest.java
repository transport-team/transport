package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Driver;
import com.transport.domain.LocationLog;
import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.LocationResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.LocationLogRepository;

// 마지막 위치 조회 (7-1 후속): 본인 고객 · 배정 기사 · 관리자만, 운송중일 때만 보여 준다
// 위치 보내기: 배정 기사 · 운송중만 저장하고 고객에게 SSE로 보낸다
// 배송 · 기사 · 배차는 약속 메서드(ShipmentService · DriverService · DispatchService)를 가짜로 둔다 (#94 저장소 직접 사용 정리)
@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

	private static final Long SHIPMENT_ID = 1L;
	private static final Long CUSTOMER_ID = 100L;       // 배송을 신청한 고객 (회원 ID)
	private static final Long DRIVER_MEMBER_ID = 200L;  // 배정 기사 (회원 ID — getActiveDriver가 주는 값)
	private static final Long OTHER_ID = 300L;          // 이 배송과 상관없는 회원

	@Mock
	LocationLogRepository locationLogRepository;

	@Mock
	ShipmentService shipmentService;

	@Mock
	DriverService driverService;

	@Mock
	NotificationServiceImpl notificationService;

	@Mock
	DispatchService dispatchService;

	@InjectMocks
	LocationServiceImpl locationService;

	private void givenShipment(ShipmentStatus status) {
		Shipment shipment = Shipment.builder()
				.id(SHIPMENT_ID)
				.status(status)
				.customer(Member.builder().id(CUSTOMER_ID).build())
				.build();
		when(shipmentService.getShipment(SHIPMENT_ID)).thenReturn(shipment);
		lenient().when(dispatchService.getActiveDriver(SHIPMENT_ID)).thenReturn(DRIVER_MEMBER_ID);
		lenient().when(locationLogRepository.findTopByShipmentIdOrderByRecordedAtDesc(SHIPMENT_ID))
				.thenReturn(Optional.of(LocationLog.builder()
						.lat(37.5665)
						.lng(126.978)
						.recordedAt(LocalDateTime.of(2026, 11, 12, 10, 0))
						.build()));
	}

	@Test
	@DisplayName("본인 고객은 운송중 배송의 마지막 위치를 본다")
	void ownerSeesLocationInTransit() {
		givenShipment(ShipmentStatus.IN_TRANSIT);

		LocationResponseDTO result = locationService.getLatestLocation(SHIPMENT_ID, CUSTOMER_ID, MemberRole.CUSTOMER);

		assertThat(result).isNotNull();
		assertThat(result.getLat()).isEqualTo(37.5665);
		assertThat(result.getLng()).isEqualTo(126.978);
	}

	@Test
	@DisplayName("배정 기사와 관리자도 볼 수 있다")
	void assignedDriverAndAdminSeeLocation() {
		givenShipment(ShipmentStatus.IN_TRANSIT);

		assertThat(locationService.getLatestLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, MemberRole.DRIVER)).isNotNull();
		assertThat(locationService.getLatestLocation(SHIPMENT_ID, OTHER_ID, MemberRole.ADMIN)).isNotNull();
	}

	@Test
	@DisplayName("다른 고객 · 배정되지 않은 기사는 403 (위치를 조회하지도 않는다)")
	void othersAreForbidden() {
		givenShipment(ShipmentStatus.IN_TRANSIT);

		CustomException otherCustomer = assertThrows(CustomException.class,
				() -> locationService.getLatestLocation(SHIPMENT_ID, OTHER_ID, MemberRole.CUSTOMER));
		CustomException otherDriver = assertThrows(CustomException.class,
				() -> locationService.getLatestLocation(SHIPMENT_ID, OTHER_ID, MemberRole.DRIVER));

		assertThat(otherCustomer.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
		assertThat(otherDriver.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
		verify(locationLogRepository, never()).findTopByShipmentIdOrderByRecordedAtDesc(anyLong());
	}

	@Test
	@DisplayName("운송중이 아니면 본인이어도 위치를 주지 않는다 (null → 204)")
	void noLocationWhenNotInTransit() {
		givenShipment(ShipmentStatus.ARRIVED);

		assertThat(locationService.getLatestLocation(SHIPMENT_ID, CUSTOMER_ID, MemberRole.CUSTOMER)).isNull();
		verify(locationLogRepository, never()).findTopByShipmentIdOrderByRecordedAtDesc(anyLong());
	}

	@Test
	@DisplayName("없는 배송은 404")
	void shipmentNotFound() {
		when(shipmentService.getShipment(SHIPMENT_ID)).thenThrow(new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

		CustomException ex = assertThrows(CustomException.class,
				() -> locationService.getLatestLocation(SHIPMENT_ID, CUSTOMER_ID, MemberRole.CUSTOMER));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHIPMENT_NOT_FOUND);
	}

	// ===== 위치 보내기 (기사) — 배정 기사 확인은 getActiveDriver의 회원 ID와 로그인 회원 ID를 비교한다 =====

	private final Driver driverEntity = Driver.builder().id(7L).build();

	@Test
	@DisplayName("배정 기사가 운송중에 보내면 그 기사로 위치를 저장한다 (SSE 전송은 NotificationServiceImplTest)")
	void assignedDriverSendsLocation() {
		givenShipment(ShipmentStatus.IN_TRANSIT);
		when(driverService.findByMemberId(DRIVER_MEMBER_ID)).thenReturn(Optional.of(driverEntity));
		when(locationLogRepository.save(any(LocationLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

		LocationResponseDTO result = locationService.sendLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, 37.5, 127.0);

		assertThat(result.getLat()).isEqualTo(37.5);
		verify(locationLogRepository).save(org.mockito.ArgumentMatchers.argThat(log -> log.getDriver() == driverEntity));
	}

	@Test
	@DisplayName("배정되지 않은 기사는 NOT_ASSIGNED_DRIVER (저장 · 전송 안 함)")
	void otherDriverCannotSend() {
		givenShipment(ShipmentStatus.IN_TRANSIT);
		when(driverService.findByMemberId(OTHER_ID)).thenReturn(Optional.of(driverEntity));

		CustomException ex = assertThrows(CustomException.class,
				() -> locationService.sendLocation(SHIPMENT_ID, OTHER_ID, 37.5, 127.0));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER);
		verify(locationLogRepository, never()).save(any(LocationLog.class));
	}

	@Test
	@DisplayName("기사가 아닌 회원은 403, 운송중이 아니면 NOT_IN_TRANSIT")
	void notDriverOrNotInTransit() {
		givenShipment(ShipmentStatus.ARRIVED);
		when(driverService.findByMemberId(CUSTOMER_ID)).thenReturn(Optional.empty());
		when(driverService.findByMemberId(DRIVER_MEMBER_ID)).thenReturn(Optional.of(driverEntity));

		CustomException notDriver = assertThrows(CustomException.class,
				() -> locationService.sendLocation(SHIPMENT_ID, CUSTOMER_ID, 37.5, 127.0));
		CustomException notInTransit = assertThrows(CustomException.class,
				() -> locationService.sendLocation(SHIPMENT_ID, DRIVER_MEMBER_ID, 37.5, 127.0));

		assertThat(notDriver.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
		assertThat(notInTransit.getErrorCode()).isEqualTo(ErrorCode.NOT_IN_TRANSIT);
		verify(locationLogRepository, never()).save(any(LocationLog.class));
	}
}
