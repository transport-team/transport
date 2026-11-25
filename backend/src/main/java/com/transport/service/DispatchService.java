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

	// 배송의 현재 활성 배차 해제 (활성 배차가 없으면 아무 작업도 하지 않음)
	void releaseDispatch(Long shipmentId);

	/**
	 * 배송의 현재 활성 배차 (기사 · 차량 정보를 보여 줄 때 — 배송 상세)
	 * 부른 쪽 트랜잭션 안에서 기사 · 회원 · 차량을 꺼내 쓴다 (팀원 3 배송 상세가 저장소 대신 부름, 2026-11-25)
	 * @return 활성 배차가 없으면 빈 값 (여러 건이면 가장 최근 배정)
	 */
	java.util.Optional<com.transport.domain.Dispatch> findActiveDispatch(Long shipmentId);
}