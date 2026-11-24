package com.transport.domain.enums;

// 알림 종류 (notification.type은 VARCHAR(30)이라 값을 더해도 DB를 고치지 않아도 된다)
public enum NotificationType {
	DISPATCHED,             // 배차 완료
	STATUS_CHANGED,         // 배송 상태 변경
	INQUIRY_ANSWERED,       // 문의 답변
	REVIEW_REQUEST,         // 리뷰 요청
	APPLICATION_REJECTED,   // 기사의 배차 지원을 고객이 거절
	DRIVER_STATUS_CHANGED   // 관리자가 기사를 승인 · 가입 거절 · 중지 · 다시 활성화
}
