package com.transport.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Dispatch;
import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchRepository;
import com.transport.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminDispatchCancellationServiceImpl implements AdminDispatchCancellationService {

    private final ShipmentRepository shipmentRepository;
    private final DispatchRepository dispatchRepository;
    private final StatusHistoryService statusHistoryService;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public void cancelDispatch(Long shipmentId, Long adminMemberId, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 180) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "180자 이내의 배차 취소 사유를 입력해 주세요.");
        }

        // 다른 관리자가 동시에 취소하거나 기사 상태 변경이 발생하는 상황에서 일관성 유지
        Shipment shipment = shipmentRepository.findByIdForUpdate(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

        // 상차 이후에는 배차만 해제할 수 없음. 주문 취소/환불과도 구분
        if (shipment.getStatus() != ShipmentStatus.ASSIGNED) {
            throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE,
                    "배차 완료 상태에서만 관리자가 배차를 취소할 수 있습니다.");
        }

        Dispatch active = dispatchRepository.findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "활성 배차 정보가 없습니다."));
        Member driverMember = active.getDriver().getMember();

        // 배송의 상태 전이 규칙(ASSIGNED → REQUESTED)을 거친다
        ShipmentStatus previousStatus = shipment.getStatus();
        shipment.changeStatus(ShipmentStatus.REQUESTED);

        int released = dispatchRepository.deactivateActiveDispatches(shipmentId, LocalDateTime.now());
        if (released == 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "해제할 활성 배차가 없습니다.");
        }

        String detail = "관리자 배차 취소: " + reason.trim();
        statusHistoryService.record(shipment, previousStatus, ShipmentStatus.REQUESTED,
                Member.builder().id(adminMemberId).build(), detail);

        String message = "배송 " + shipmentId + "번의 배차가 관리자에 의해 해제되었습니다. 사유: " + reason.trim();
        notificationService.send(driverMember, NotificationType.STATUS_CHANGED, message, shipment);
        notificationService.send(shipment.getCustomer(), NotificationType.STATUS_CHANGED, message, shipment);
    }
}
