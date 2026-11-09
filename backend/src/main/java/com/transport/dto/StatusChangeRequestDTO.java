package com.transport.dto;

import com.transport.domain.enums.ShipmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatusChangeRequestDTO {

	@NotNull(message = "다음 상태는 필수입니다.")
	private ShipmentStatus nextStatus;

	private String memo;
}