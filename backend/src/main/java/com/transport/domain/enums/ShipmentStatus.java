package com.transport.domain.enums;

// 배송 상태 (docs/status.md). 전이 규칙 canChangeTo()는 기능 구현 단계에서 추가한다
public enum ShipmentStatus {
	PENDING_PAYMENT,   // 결제대기
	REQUESTED,         // 접수 (결제 완료, 배차 대기)
	ASSIGNED,          // 배차완료
	PICKED_UP,         // 상차완료
	IN_TRANSIT,        // 운송중 (이때만 위치 공유)
	ARRIVED,           // 도착
	COMPLETED,         // 배송완료
	CANCELED           // 취소
}
