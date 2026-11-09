package com.transport.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShipmentResponseDTO {
    private Long shipmentId;
    private String status;
    private String orderId;
    private int amount;
}