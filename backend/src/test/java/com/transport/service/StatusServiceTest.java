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
import com.transport.repository.ShipmentImageRepository;
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

	@Mock
	ShipmentImageRepository shipmentImageRepository; // 5-2 사진 필수 검사용

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
		// 5-2 사진 필수 검사: 기본은 "사진 있음"으로 통과, 사진 누락 테스트에서만 false로 오버라이드
		lenient().when(shipmentImageRepository.existsPickupImage(1L)).thenReturn(true);
		lenient().when(shipmentImageRepository.existsReceiptImage(1L)).thenReturn(true);
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

	// ===== 5-2 사진 필수 검사 =====

	@Test
	@DisplayName("예외: 상차 처리인데 PICKUP 사진이 없음 (PHOTO_REQUIRED)")
	void changeStatus_pickupWithoutPhoto() {
		givenShipment(ShipmentStatus.ASSIGNED, 10L);
		when(shipmentImageRepository.existsPickupImage(1L)).thenReturn(false); // 사진 없음

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "", 10L),
				CustomException.class);

		org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PHOTO_REQUIRED);
		org.assertj.core.api.Assertions.assertThat(ex.getMessage()).isEqualTo("상차 사진을 먼저 업로드해 주세요.");

		verify(shipment, never()).updateStatus(any(ShipmentStatus.class));
		verify(statusHistoryService, never()).record(any(), any(), any(), any(), anyString());
	}

	@Test
	@DisplayName("예외: 배송완료 처리인데 RECEIPT(인수증) 사진이 없음 (PHOTO_REQUIRED)")
	void changeStatus_completeWithoutReceipt() {
		givenShipment(ShipmentStatus.ARRIVED, 10L);
		when(shipmentImageRepository.existsReceiptImage(1L)).thenReturn(false); // 인수증 없음

		CustomException ex = org.assertj.core.api.Assertions.catchThrowableOfType(
				() -> statusService.changeStatus(1L, ShipmentStatus.COMPLETED, "", 10L),
				CustomException.class);

		org.assertj.core.api.Assertions.assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.PHOTO_REQUIRED);

		verify(shipment, never()).updateStatus(any(ShipmentStatus.class));
	}

	@Test
	@DisplayName("정상: PICKUP 사진이 있으면 상차 처리 성공")
	void changeStatus_pickupWithPhoto() {
		givenShipment(ShipmentStatus.ASSIGNED, 10L);
		when(shipmentImageRepository.existsPickupImage(1L)).thenReturn(true); // 사진 있음

		statusService.changeStatus(1L, ShipmentStatus.PICKED_UP, "상차 완료", 10L);

		verify(shipment).updateStatus(ShipmentStatus.PICKED_UP);
		verify(statusHistoryService).record(eq(shipment), eq(ShipmentStatus.ASSIGNED),
				eq(ShipmentStatus.PICKED_UP), any(), anyString());
	}
}