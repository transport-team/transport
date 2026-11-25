package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Dispatch;
import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.repository.DispatchRepository;

@ExtendWith(MockitoExtension.class)
class AdminDispatchCancellationServiceImplTest {

    @Mock private ShipmentService shipmentService;
    @Mock private DispatchRepository dispatchRepository;
    @Mock private StatusHistoryService statusHistoryService;
    @Mock private NotificationService notificationService;
    @InjectMocks private AdminDispatchCancellationServiceImpl service;

    @Test
    void cancelAssignedDispatchKeepsOrderAndNotifiesBoth() {
        Member customer = Member.builder().id(4L).build();
        Member driverMember = Member.builder().id(5L).build();
        Shipment shipment = Shipment.builder().id(10L).customer(customer).status(ShipmentStatus.ASSIGNED).build();
        Dispatch dispatch = Dispatch.builder()
                .driver(Driver.builder().member(driverMember).build())
                .active(true).assignedAt(LocalDateTime.now()).build();

        when(shipmentService.getShipmentForUpdate(10L)).thenReturn(shipment);
        when(dispatchRepository.findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L))
                .thenReturn(Optional.of(dispatch));
        when(dispatchRepository.deactivateActiveDispatches(eq(10L), any(LocalDateTime.class))).thenReturn(1);

        service.cancelDispatch(10L, 7L, "기사 요청으로 재배차");

        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.REQUESTED);
        verify(dispatchRepository).deactivateActiveDispatches(eq(10L), any(LocalDateTime.class));
        verify(statusHistoryService).record(eq(shipment), eq(ShipmentStatus.ASSIGNED),
                eq(ShipmentStatus.REQUESTED), any(Member.class), eq("관리자 배차 취소: 기사 요청으로 재배차"));
        verify(notificationService).send(eq(customer), eq(NotificationType.STATUS_CHANGED), any(String.class), eq(shipment));
        verify(notificationService).send(eq(driverMember), eq(NotificationType.STATUS_CHANGED), any(String.class), eq(shipment));
    }

    @Test
    void cannotCancelAfterPickup() {
        Shipment shipment = Shipment.builder().id(10L).status(ShipmentStatus.PICKED_UP).build();
        when(shipmentService.getShipmentForUpdate(10L)).thenReturn(shipment);

        assertThatThrownBy(() -> service.cancelDispatch(10L, 7L, "배차 해제"))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(dispatchRepository, statusHistoryService, notificationService);
    }

    @Test
    void cannotCancelWithoutReason() {
        assertThatThrownBy(() -> service.cancelDispatch(10L, 7L, "  "))
                .isInstanceOf(CustomException.class);
        verifyNoInteractions(shipmentService, dispatchRepository);
    }

    @Test
    void cannotCancelWithoutActiveDispatch() {
        Shipment shipment = Shipment.builder().id(10L).status(ShipmentStatus.ASSIGNED).build();
        when(shipmentService.getShipmentForUpdate(10L)).thenReturn(shipment);
        when(dispatchRepository.findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(10L))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.cancelDispatch(10L, 7L, "배차 해제"))
                .isInstanceOf(CustomException.class);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.ASSIGNED);
        verify(notificationService, never()).send(any(), any(), any(), any());
    }
}
