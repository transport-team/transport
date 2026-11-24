package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.enums.DriverStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 기사 정보 (관리자 기사 목록)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverDTO {

	private Long id;
	private Long memberId;
	private String name;
	private String email;
	private String phone;
	private String licenseType;
	private int careerYears;
	private DriverStatus status;
	private LocalDateTime approvedAt;  // 처음 승인한 시각 (승인 전이면 null)
	private LocalDateTime createdAt;   // 가입 시각
}
