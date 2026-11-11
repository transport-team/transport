package com.transport.service;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.ShipmentStatusHistoryResponseDTO;
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
import static org.assertj.core.api.Assertions.assertThat;

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

	Shipment shipment; // 실제 배송 객체로 상태 변경 규칙 검증

	@InjectMocks
	StatusServiceImpl statusService;

    private void givenShipment(ShipmentStatus status, Long driverId) {
            // 실제 Shipment 객체 생성
            shipment = Shipment.builder()
                            .id(1L)
                            .status(status)
                            .customer(Member.builder().id(100L).build())
                            .build();

            when(shipmentRepository.findById(1L))
                            .thenReturn(Optional.of(shipment));

            lenient().when(dispatchService.getActiveDriver(1L))
                            .thenReturn(driverId);
    }

	@Test
	@DisplayName("정상: 배정 기사가 상태 변경 성공")
	void changeStatus_success() {
		givenShipment(ShipmentStatus.ASSIGNED, 10L);

		statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "상차 완료", 10L);

		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.PICKED_UP);
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

		// 잘못된 상태 변경을 시도해도 기존 상태 유지
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.REQUESTED);
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

		// 권한이 없으면 배송 상태가 변경되지 않음
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
	}

	@Test
	@DisplayName("예외: 배정 기사 없음 (null 반환)")
	void changeStatus_noAssignedDriver() {
		givenShipment(ShipmentStatus.ASSIGNED, null);

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "", 10L),
				CustomException.class);

		org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER);

		// 권한이 없으면 배송 상태가 변경되지 않음
		assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
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

	// ===== 4-4 이력 조회 (getHistory) =====

	@Test
	@DisplayName("이력 조회 정상: 본인 고객")
	void getHistory_ownerSuccess() {
		givenShipment(ShipmentStatus.ASSIGNED, 10L); // customer = 100
		ShipmentStatusHistory history = ShipmentStatusHistory.builder()
				.shipment(shipment)
				.fromStatus(ShipmentStatus.REQUESTED)
				.toStatus(ShipmentStatus.ASSIGNED)
				.changedBy(null)
				.memo("배차 완료")
				.changedAt(java.time.LocalDateTime.now())
				.build();
		when(statusHistoryService.getHistory(1L)).thenReturn(List.of(history));

		List<ShipmentStatusHistoryResponseDTO> result =
				statusService.getHistory(1L, 100L, MemberRole.CUSTOMER);

		assertThat(result).hasSize(1);
		assertThat(result.get(0).getToStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
		assertThat(result.get(0).getChangedByName()).isNull(); // 시스템 변경자
	}

	@Test
	@DisplayName("이력 조회 정상: 관리자")
	void getHistory_adminSuccess() {
		givenShipment(ShipmentStatus.ASSIGNED, 10L);
		when(statusHistoryService.getHistory(1L)).thenReturn(List.of());

		List<ShipmentStatusHistoryResponseDTO> result =
				statusService.getHistory(1L, 999L, MemberRole.ADMIN);

		assertThat(result).isEmpty();
	}

	@Test
	@DisplayName("이력 조회 예외: 관련 없는 제3자 (FORBIDDEN)")
	void getHistory_forbidden() {
		givenShipment(ShipmentStatus.ASSIGNED, 10L); // customer=100, 배정기사=10, 요청자=999

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.getHistory(1L, 999L, MemberRole.CUSTOMER),
				CustomException.class);

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
		verify(statusHistoryService, never()).getHistory(anyLong());
	}
}