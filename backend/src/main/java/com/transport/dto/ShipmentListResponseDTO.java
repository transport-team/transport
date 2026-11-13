package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShipmentListResponseDTO {

    private Long id;
    private ShipmentStatus status;
    private String originAddress;
    private String destAddress;
    private String cargoName;
    private int fare;
    private LocalDateTime pickupAt;
    private LocalDateTime createdAt;

    public static ShipmentListResponseDTO from(Shipment shipment) {
        return ShipmentListResponseDTO.builder()
                .id(shipment.getId())
                .status(shipment.getStatus())
                .originAddress(shipment.getOriginAddress())
                .destAddress(shipment.getDestAddress())
                .cargoName(shipment.getCargoName())
                .fare(shipment.getFare())
                .pickupAt(shipment.getPickupAt())
                .createdAt(shipment.getCreatedAt())
                .build();
    }
}
