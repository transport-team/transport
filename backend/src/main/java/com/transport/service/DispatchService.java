package com.transport.service;

// 배차 서비스 (팀원 2 구현 예정 — 5주차, docs/collaboration.md)
// 4주차에는 팀원 4가 인터페이스만 정의해 목 처리용으로 사용
public interface DispatchService {

	/**
	 * 배송에 배정된 현재 활성 기사의 회원 ID(member.id) 반환
	 * (약속 표 5주차: driver.id가 아니라 member.id — 상태 변경 · 이력 조회가 로그인 회원 ID와 비교한다)
	 * @param shipmentId 배송 ID
	 * @return 기사의 member.id (배정 없으면 null)
	 */
	Long getActiveDriver(Long shipmentId);
}