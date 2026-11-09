package com.transport.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CargoTypeResponseDTO {

	private Long id;
	private String name;
	private int extraFee;
	private String handlingNote;
	private boolean active;
}
