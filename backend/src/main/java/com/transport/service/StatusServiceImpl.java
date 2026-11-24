package com.transport.service;

import java.util.List;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.ShipmentStatusHistoryResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentImageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
@Log4j2
public class StatusServiceImpl implements StatusService {

	private final ShipmentService shipmentService;  // 배송 = 팀원 3 약속 메서드 (저장소 직접 사용 정리, #94)
	private final ShipmentImageRepository shipmentImageRepository;
	private final com.transport.repository.ShipmentRepository shipmentRepository;
	private final DispatchService dispatchService;
	private final StatusHistoryService statusHistoryService;
	private final NotificationService notificationService;

	@Override
	public void changeStatus(Long shipmentId, ShipmentStatus nextStatus, String memo, Long driverId) {
		Shipment shipment = shipmentService.getShipment(shipmentId);  // 같은 트랜잭션의 엔티티라 상태 변경이 그대로 저장된다

		// 1. 배정 기사 확인 (DispatchService — 현재 목, 5주차에 팀원2 실구현으로 교체)
		Long assignedDriverId = dispatchService.getActiveDriver(shipmentId);
		if (assignedDriverId == null || !assignedDriverId.equals(driverId)) {
			throw new CustomException(ErrorCode.NOT_ASSIGNED_DRIVER);
		}

        // 2. 변경 전 배송 상태 저장
        ShipmentStatus fromStatus = shipment.getStatus();
        // 기사 배송완료 이후에는 고객만 최종 완료를 수행할 수 있다.
        if (fromStatus == ShipmentStatus.DELIVERED && nextStatus == ShipmentStatus.COMPLETED) {
            throw new CustomException(ErrorCode.FORBIDDEN,
                    "최종 운송 완료는 고객 수령 확인으로만 처리할 수 있습니다.");
        }

        // 3. 사진 필수 검사 (상차 사진 / 인수증 사진)
        validatePhotoRequired(shipmentId, fromStatus, nextStatus);

        // 4. 공통 상태 전이 규칙 검사 후 상태 변경 및 이력 저장
        shipment.changeStatus(nextStatus);
        Member driver = Member.builder().id(driverId).build(); // 최소 필드만 (변경자 기록용)
        statusHistoryService.record(shipment, fromStatus, nextStatus, driver, memo);

		// 5. 알림 전송 (고객에게)
		NotificationType type = mapNotificationType(nextStatus);
		String message = buildMessage(nextStatus, shipment.getId());
		notificationService.send(shipment.getCustomer(), type, message, shipment);

		log.info("배송 상태 변경: shipmentId={}, {} -> {}, driverId={}", shipmentId, fromStatus, nextStatus, driverId);
	}

    @Override
    public void confirmReceipt(Long shipmentId, Long customerMemberId) {
        // 비관적 락: 수령 확정 중복 클릭 시 동일한 배송을 동시에 완료하지 못하도록 한다.
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));
        if (shipment.getCustomer() == null || !shipment.getCustomer().getId().equals(customerMemberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "배송 신청 고객만 수령을 확정할 수 있습니다.");
        }
        if (shipment.getStatus() != ShipmentStatus.DELIVERED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE,
                    "기사 배송 완료 후에만 수령을 확정할 수 있습니다. 이미 확정된 배송인지 확인해 주세요.");
        }
        shipment.changeStatus(ShipmentStatus.COMPLETED);
        statusHistoryService.record(shipment, ShipmentStatus.DELIVERED, ShipmentStatus.COMPLETED,
                shipment.getCustomer(), "고객 수령 확인 · 운송 최종 완료");
        Long driverMemberId = dispatchService.getActiveDriver(shipmentId);
        if (driverMemberId != null) {
            // 배정 기사 알림은 회원 조회가 아닌 기존 영속성 참조로 전달한다.
            notificationService.send(Member.builder().id(driverMemberId).build(),
                    NotificationType.STATUS_CHANGED,
                    "고객이 수령을 확정했습니다: 배송 " + shipmentId, shipment);
        }
        log.info("고객 수령 확정: shipmentId={}, customerMemberId={}", shipmentId, customerMemberId);
    }

	@Override
	@Transactional(readOnly = true)
	public List<ShipmentStatusHistoryResponseDTO> getHistory(Long shipmentId, Long memberId, MemberRole role) {
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 권한: 관리자 · 본인 고객 · 배정 기사만 (requirements.md — 배송 상세 접근 제한)
		boolean isAdmin = role == MemberRole.ADMIN;
		boolean isOwner = shipment.getCustomer() != null
				&& shipment.getCustomer().getId().equals(memberId);
		Long assignedDriverId = dispatchService.getActiveDriver(shipmentId);
		boolean isAssignedDriver = assignedDriverId != null && assignedDriverId.equals(memberId);

		if (!isAdmin && !isOwner && !isAssignedDriver) {
			throw new CustomException(ErrorCode.FORBIDDEN, "본인 배송만 조회할 수 있습니다.");
		}

		return statusHistoryService.getHistory(shipmentId).stream()
				.map(ShipmentStatusHistoryResponseDTO::from)
				.toList();
	}

    /** 사진 필수 검사 — ASSIGNED→PICKED_UP은 PICKUP 사진, ARRIVED→DELIVERED는 RECEIPT 사진 (5-2) */
    private void validatePhotoRequired(Long shipmentId, ShipmentStatus from, ShipmentStatus to) {
            if (from == ShipmentStatus.ASSIGNED && to == ShipmentStatus.PICKED_UP
                            && !shipmentImageRepository.existsPickupImage(shipmentId)) {
                    throw new CustomException(ErrorCode.PHOTO_REQUIRED, "상차 사진을 먼저 업로드해 주세요.");
            }
            if (from == ShipmentStatus.ARRIVED && to == ShipmentStatus.DELIVERED
                            && !shipmentImageRepository.existsReceiptImage(shipmentId)) {
                    throw new CustomException(ErrorCode.PHOTO_REQUIRED, "인수증 사진을 먼저 업로드해 주세요.");
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
			case DELIVERED -> "기사가 배송을 완료했어요. 수령 확인을 진행해 주세요: 배송 " + shipmentId;
			case COMPLETED -> "운송이 최종 완료됐어요: 배송 " + shipmentId;
			default -> "상태가 변경됐어요: 배송 " + shipmentId;
		};
	}
}