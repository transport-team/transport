package com.transport.service;

import java.util.List;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;

// 배송 상태 변경 이력 저장 (팀원 4 — docs/collaboration.md 약속: 모든 도메인이 이 메서드로만 이력 기록)
public interface StatusHistoryService {

	/**
	 * 상태 변경 이력 한 건 저장
	 * @param shipment 대상 배송
	 * @param from 이전 상태 (최초 신청 시 null)
	 * @param to 다음 상태
	 * @param changedBy 변경자 (시스템이면 null)
	 * @param memo 사유
	 * @return 저장된 이력 엔티티
	 */
	ShipmentStatusHistory record(Shipment shipment, ShipmentStatus from, ShipmentStatus to, Member changedBy, String memo);

	/**
	 * 배송의 상태 변경 이력 목록 (오래된 순 — 타임라인 표시용)
	 * @param shipmentId 배송 ID
	 * @return 이력 목록 (없으면 빈 목록)
	 */
	List<ShipmentStatusHistory> getHistory(Long shipmentId);
}
