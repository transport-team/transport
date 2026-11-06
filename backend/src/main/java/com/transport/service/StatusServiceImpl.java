package com.transport.service;

import java.util.Map;
import java.util.Set;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class StatusServiceImpl implements StatusService {

	private final ShipmentRepository shipmentRepository;
	private final DispatchService dispatchService;
	private final StatusHistoryService statusHistoryService;
	private final NotificationService notificationService;

	// 임시 전이 규칙 (docs/status.md) — TODO: 팀원3이 ShipmentStatus.canChangeTo() + Shipment.changeStatus()를
	// 머지하면 이 상수와 validateTransition()을 제거하고 shipment.changeStatus(nextStatus) 한 줄로 교체한다
	private static final Map<ShipmentStatus, Set<ShipmentStatus>> NEXT = Map.of(
			ShipmentStatus.PENDING_PAYMENT, Set.of(ShipmentStatus.REQUESTED, ShipmentStatus.CANCELED),
			ShipmentStatus.REQUESTED, Set.of(ShipmentStatus.ASSIGNED, ShipmentStatus.CANCELED),
			ShipmentStatus.ASSIGNED, Set.of(ShipmentStatus.PICKED_UP, ShipmentStatus.REQUESTED, ShipmentStatus.CANCELED),
			ShipmentStatus.PICKED_UP, Set.of(ShipmentStatus.IN_TRANSIT),
			ShipmentStatus.IN_TRANSIT, Set.of(ShipmentStatus.ARRIVED),
			ShipmentStatus.ARRIVED, Set.of(ShipmentStatus.COMPLETED)
	);

	@Override
	public void changeStatus(Long shipmentId, ShipmentStatus nextStatus, String memo, Long driverId) {
		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

		// 1. 배정 기사 확인 (DispatchService — 현재 목, 5주차에 팀원2 실구현으로 교체)
		Long assignedDriverId = dispatchService.getActiveDriver(shipmentId);
		if (assignedDriverId == null || !assignedDriverId.equals(driverId)) {
			throw new CustomException(ErrorCode.NOT_ASSIGNED_DRIVER);
		}

		// 2. 상태 변경 (임시 검증 → 팀원3 canChangeTo() 머지 시 교체)
		ShipmentStatus fromStatus = shipment.getStatus();
		validateTransition(fromStatus, nextStatus);
		shipment.updateStatus(nextStatus);

		// 3. 이력 저장
		Member driver = Member.builder().id(driverId).build(); // 최소 필드만 (변경자 기록용)
		statusHistoryService.record(shipment, fromStatus, nextStatus, driver, memo);

		// 4. 알림 전송 (고객에게)
		NotificationType type = mapNotificationType(nextStatus);
		String message = buildMessage(nextStatus, shipment.getId());
		notificationService.send(shipment.getCustomer(), type, message, shipment);

		log.info("배송 상태 변경: shipmentId={}, {} -> {}, driverId={}", shipmentId, fromStatus, nextStatus, driverId);
	}

	/** 임시 전이 검증 — 팀원3의 canChangeTo() 머지 시 제거 */
	private void validateTransition(ShipmentStatus from, ShipmentStatus to) {
		Set<ShipmentStatus> allowed = NEXT.get(from);
		if (allowed == null || !allowed.contains(to)) {
			throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
		}
	}

	private NotificationType mapNotificationType(ShipmentStatus status) {
		return switch (status) {
			case ASSIGNED -> NotificationType.DISPATCHED;
			default -> NotificationType.STATUS_CHANGED;
		};
	}

	private String buildMessage(ShipmentStatus status, Long shipmentId) {
		return switch (status) {
			case PICKED_UP -> "상차가 완료됐어요: 배송 " + shipmentId;
			case IN_TRANSIT -> "운송이 시작됐어요: 배송 " + shipmentId;
			case ARRIVED -> "도착했어요: 배송 " + shipmentId;
			case COMPLETED -> "배송이 완료됐어요: 배송 " + shipmentId;
			default -> "상태가 변경됐어요: 배송 " + shipmentId;
		};
	}
}