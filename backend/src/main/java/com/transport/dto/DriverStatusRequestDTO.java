package com.transport.dto;

import com.transport.domain.enums.DriverStatus;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 기사 승인 · 중지 요청 { "status": "ACTIVE" | "INACTIVE" }
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverStatusRequestDTO {

	@NotNull(message = "변경할 상태를 선택해 주세요.")
	private DriverStatus status;
}
