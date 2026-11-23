package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.repository.ShipmentRepository;
import com.transport.repository.ShipmentImageRepository;

@ExtendWith(MockitoExtension.class)
class CustomerReceiptConfirmationTest {
    @Mock ShipmentRepository shipmentRepository;
    @Mock ShipmentService shipmentService;
    @Mock ShipmentImageRepository shipmentImageRepository;
    @Mock DispatchService dispatchService;
    @Mock StatusHistoryService statusHistoryService;
    @Mock NotificationService notificationService;
    @InjectMocks StatusServiceImpl statusService;

    private Shipment shipment;

    @BeforeEach
    void setUp() {
        shipment = Shipment.builder()
                .id(77L).customer(Member.builder().id(10L).build())
                .status(ShipmentStatus.DELIVERED).build();
    }

    @Test
    void customerCanConfirmDeliveredShipment() {
        when(shipmentRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(shipment));
        statusService.confirmReceipt(77L, 10L);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.COMPLETED);
        verify(statusHistoryService).record(eq(shipment), eq(ShipmentStatus.DELIVERED),
                eq(ShipmentStatus.COMPLETED), any(Member.class), anyString());
    }

    @Test
    void otherCustomerCannotConfirm() {
        when(shipmentRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(shipment));
        assertThatThrownBy(() -> statusService.confirmReceipt(77L, 99L))
                .isInstanceOf(CustomException.class);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
    }

    @Test
    void duplicateConfirmationIsRejected() {
        shipment.changeStatus(ShipmentStatus.COMPLETED);
        when(shipmentRepository.findByIdForUpdate(77L)).thenReturn(Optional.of(shipment));
        assertThatThrownBy(() -> statusService.confirmReceipt(77L, 10L))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void assignedDriverCannotBypassCustomerConfirmation() {
        when(shipmentService.getShipment(77L)).thenReturn(shipment);
        when(dispatchService.getActiveDriver(77L)).thenReturn(20L);
        assertThatThrownBy(() -> statusService.changeStatus(77L, ShipmentStatus.COMPLETED, "", 20L))
                .isInstanceOf(CustomException.class);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.DELIVERED);
        verifyNoInteractions(statusHistoryService);
    }
}
