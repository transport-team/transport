package com.transport.dto;

import com.transport.domain.enums.VehicleStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VehicleResponseDTO {

	private Long id;

	private String plateNo;

	private Long vehicleTypeId;

	private String vehicleTypeName;

	private VehicleStatus status;

	private Long driverId;

}
