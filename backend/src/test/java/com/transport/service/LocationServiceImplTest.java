package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

import com.transport.domain.LocationLog;
import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.LocationResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.LocationLogRepository;
import com.transport.repository.ShipmentRepository;

// 마지막 위치 조회 (7-1 후속): 본인 고객 · 배정 기사 · 관리자만, 운송중일 때만 보여 준다
@ExtendWith(MockitoExtension.class)
class LocationServiceImplTest {

	private static final Long SHIPMENT_ID = 1L;
	private static final Long CUSTOMER_ID = 100L;       // 배송을 신청한 고객 (회원 ID)
	private static final Long DRIVER_MEMBER_ID = 200L;  // 배정 기사 (회원 ID — getActiveDriver가 주는 값)
	private static final Long OTHER_ID = 300L;          // 이 배송과 상관없는 회원

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
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
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
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.empty());

		CustomException ex = assertThrows(CustomException.class,
				() -> locationService.getLatestLocation(SHIPMENT_ID, CUSTOMER_ID, MemberRole.CUSTOMER));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SHIPMENT_NOT_FOUND);
	}
}
