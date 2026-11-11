package com.transport.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

// 배송 상태 전이 규칙 테스트 (실제 DB 사용 안 함)
class ShipmentStatusTransitionTest {

    // 접수 -> 배차 완료는 허용
    @Test
    void requestedToAssigned() {
        Shipment shipment = Shipment.builder()
                .status(ShipmentStatus.REQUESTED)
                .build();

        shipment.changeStatus(ShipmentStatus.ASSIGNED);

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.ASSIGNED);
    }

    // 접수 -> 배송 완료는 허용하지 않음
    @Test
    void requestedToCompletedNotAllowed() {
        Shipment shipment = Shipment.builder()
                .status(ShipmentStatus.REQUESTED)
                .build();

        assertThatThrownBy(() ->
                shipment.changeStatus(ShipmentStatus.COMPLETED))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_CHANGE);

        // 실패하면 기존 상태 유지
        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.REQUESTED);
    }

    // 배차 해제 시 배차 완료 -> 접수는 허용
    @Test
    void assignedToRequested() {
        Shipment shipment = Shipment.builder()
                .status(ShipmentStatus.ASSIGNED)
                .build();

        shipment.changeStatus(ShipmentStatus.REQUESTED);

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.REQUESTED);
    }

    // 배송 완료 후에는 상태를 변경할 수 없음
    @Test
    void completedCannotChangeStatus() {
        Shipment shipment = Shipment.builder()
                .status(ShipmentStatus.COMPLETED)
                .build();

        assertThatThrownBy(() ->
                shipment.changeStatus(ShipmentStatus.REQUESTED))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_STATUS_CHANGE);
    }
}
