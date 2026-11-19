package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminShipmentResponseDTO {

	private Long id;
	private ShipmentStatus status;
	private Long customerId;
	private String customerName;
	private String customerEmail;
	private String vehicleTypeName;
	private String cargoTypeName;
	private String cargoName;
	private int cargoWeightKg;
	private String originAddress;
	private String destAddress;
	private double distanceKm;
	private int fare;
	private String receiverName;
	private LocalDateTime pickupAt;
	private LocalDateTime createdAt;

	public static AdminShipmentResponseDTO from(Shipment shipment) {
		return AdminShipmentResponseDTO.builder()
				.id(shipment.getId())
				.status(shipment.getStatus())
				.customerId(shipment.getCustomer().getId())
				.customerName(shipment.getCustomer().getName())
				.customerEmail(shipment.getCustomer().getEmail())
				.vehicleTypeName(shipment.getVehicleType().getName())
				.cargoTypeName(shipment.getCargoType().getName())
				.cargoName(shipment.getCargoName())
				.cargoWeightKg(shipment.getCargoWeightKg())
				.originAddress(shipment.getOriginAddress())
				.destAddress(shipment.getDestAddress())
				.distanceKm(shipment.getDistanceKm())
				.fare(shipment.getFare())
				.receiverName(shipment.getReceiverName())
				.pickupAt(shipment.getPickupAt())
				.createdAt(shipment.getCreatedAt())
				.build();
	}
}