package com.transport.dto;

import com.transport.domain.enums.VehicleStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VehicleRequestDTO {

	@NotBlank
	@Size(max = 20)
	private String plateNo;

	@NotNull
	@Positive
	private Long vehicleTypeId;

	@NotNull
	private VehicleStatus status;

	@Positive
	private Long driverId;

}
