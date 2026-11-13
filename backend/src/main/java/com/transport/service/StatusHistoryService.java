package com.transport.service;

import java.time.LocalDateTime;
import java.util.List;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverCompletedStatDTO;

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

	// ===== 관리자 대시보드 집계 (읽기 전용 — 팀원 1 작성 · 팀원 4 리뷰, docs/decisions.md 2026-11-13) =====

	/**
	 * 기간 [from, to) 안에 배송완료(COMPLETED)로 바뀐 건수 — 대시보드 "오늘 완료"
	 * 완료는 끝 상태라 배송마다 이력이 한 번만 남는다
	 */
	long countCompleted(LocalDateTime from, LocalDateTime to);

	/**
	 * 기간 [from, to) 안의 기사별 배송완료 건수 — 대시보드 "기사 순위"
	 * 완료는 배정 기사만 바꿀 수 있어 이력의 변경자가 곧 그 기사다 (status.md)
	 * @param limit 몇 명까지 (1 이상)
	 * @return 건수 많은 순, 같으면 기사 id 순 (없으면 빈 목록)
	 */
	List<DriverCompletedStatDTO> getCompletedCountsByDriver(LocalDateTime from, LocalDateTime to, int limit);
}
