package com.transport.service;

import java.util.List;

import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.ShipmentStatusHistoryResponseDTO;

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

	/**
	 * 배송 상태 이력 조회 — 본인 고객 · 배정 기사 · 관리자만 (requirements.md 비기능 요구사항)
	 * @param shipmentId 배송 ID
	 * @param memberId 요청자 회원 ID
	 * @param role 요청자 역할
	 * @return 이력 목록 (오래된 순)
	 */
	List<ShipmentStatusHistoryResponseDTO> getHistory(Long shipmentId, Long memberId, MemberRole role);
}