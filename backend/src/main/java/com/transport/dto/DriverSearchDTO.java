package com.transport.dto;

import com.transport.domain.enums.DriverStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

// 관리자 기사 목록 검색 (?status=PENDING&keyword=김&page=1)
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class DriverSearchDTO extends PageRequestDTO {

	private DriverStatus status;  // 없으면 전체
	private String keyword;       // 이름 · 이메일 일부
}
