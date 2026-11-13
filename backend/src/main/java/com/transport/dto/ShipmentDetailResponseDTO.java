package com.transport.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShipmentDetailResponseDTO {

    private Long id;
    private ShipmentStatus status;
    private String originAddress;
    private String originDetail;
    private double originLat;
    private double originLng;
    private String destAddress;
    private String destDetail;
    private double destLat;
    private double destLng;
    private double distanceKm;
    private Integer durationMin;
    private String cargoName;
    private int cargoWeightKg;
    private String cargoTypeName;
    private String vehicleTypeName;
    private LocalDateTime pickupAt;
    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    private String memo;
    private int fare;
    private Long assignedDriverMemberId;
    private List<ShipmentStatusHistoryResponseDTO> history;

    public static ShipmentDetailResponseDTO from(Shipment shipment,
            Long assignedDriverMemberId, List<ShipmentStatusHistoryResponseDTO> history) {
        return ShipmentDetailResponseDTO.builder()
                .id(shipment.getId())
                .status(shipment.getStatus())
                .originAddress(shipment.getOriginAddress())
                .originDetail(shipment.getOriginDetail())
                .originLat(shipment.getOriginLat())
                .originLng(shipment.getOriginLng())
                .destAddress(shipment.getDestAddress())
                .destDetail(shipment.getDestDetail())
                .destLat(shipment.getDestLat())
                .destLng(shipment.getDestLng())
                .distanceKm(shipment.getDistanceKm())
                .durationMin(shipment.getDurationMin())
                .cargoName(shipment.getCargoName())
                .cargoWeightKg(shipment.getCargoWeightKg())
                .cargoTypeName(shipment.getCargoType().getName())
                .vehicleTypeName(shipment.getVehicleType().getName())
                .pickupAt(shipment.getPickupAt())
                .senderName(shipment.getSenderName())
                .senderPhone(shipment.getSenderPhone())
                .receiverName(shipment.getReceiverName())
                .receiverPhone(shipment.getReceiverPhone())
                .memo(shipment.getMemo())
                .fare(shipment.getFare())
                .assignedDriverMemberId(assignedDriverMemberId)
                .history(history)
                .build();
    }
}
