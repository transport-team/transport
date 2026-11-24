package com.transport.domain.enums;

// 기사 상태
public enum DriverStatus {
	PENDING,   // 가입 후 승인 대기
	ACTIVE,    // 승인 · 배차 가능
	INACTIVE   // 비활성
}
