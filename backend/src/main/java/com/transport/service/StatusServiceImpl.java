package com.transport.service;

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

	@Override
	public void changeStatus(Long shipmentId, ShipmentStatus nextStatus, String memo, Long driverId) {
		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

		// 1. 배정 기사 확인
		Long assignedDriverId = dispatchService.getActiveDriver(shipmentId);
		if (assignedDriverId == null || !assignedDriverId.equals(driverId)) {
			throw new CustomException(ErrorCode.NOT_ASSIGNED_DRIVER);
		}

		// 2. 상태 변경 (Shipment.changeStatus() 내부에서 canChangeTo 검증)
		ShipmentStatus fromStatus = shipment.getStatus();
		shipment.changeStatus(nextStatus);

		// 3. 이력 저장
		Member driver = Member.builder().id(driverId).build(); // 최소 필드만
		statusHistoryService.record(shipment, fromStatus, nextStatus, driver, memo);

		// 4. 알림 전송 (고객에게)
		NotificationType type = mapNotificationType(nextStatus);
		String message = buildMessage(nextStatus, shipment.getId());
		notificationService.send(shipment.getCustomer(), type, message, shipment);

		log.info("배송 상태 변경: shipmentId={}, {} -> {}, driverId={}", shipmentId, fromStatus, nextStatus, driverId);
	}

	private NotificationType mapNotificationType(ShipmentStatus status) {
		return switch (status) {
			case ASSIGNED -> NotificationType.DISPATCHED;
			case PICKED_UP, IN_TRANSIT, ARRIVED, COMPLETED -> NotificationType.STATUS_CHANGED;
			default -> NotificationType.STATUS_CHANGED;
		};
	}

	private String buildMessage(ShipmentStatus status, Long shipmentId) {
		return switch (status) {
			case ASSIGNED -> "배차가 완료됐어요: 배송 " + shipmentId;
			case PICKED_UP -> "상차가 완료됐어요: 배송 " + shipmentId;
			case IN_TRANSIT -> "운송이 시작됐어요: 배송 " + shipmentId;
			case ARRIVED -> "도착했어요: 배송 " + shipmentId;
			case COMPLETED -> "배송이 완료됐어요: 배송 " + shipmentId;
			default -> "상태가 변경됐어요: 배송 " + shipmentId;
		};
	}
}