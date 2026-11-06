package com.transport.service;

import com.transport.domain.enums.ShipmentStatus;

// 배송 상태 변경 서비스 (팀원 4 — docs/collaboration.md)
public interface StatusService {

	/**
	 * 배송 상태 변경
	 * @param shipmentId 배송 ID
	 * @param nextStatus 다음 상태
	 * @param memo 변경 사유
	 * @param driverId 요청한 기사 ID
	 */
	void changeStatus(Long shipmentId, ShipmentStatus nextStatus, String memo, Long driverId);
}