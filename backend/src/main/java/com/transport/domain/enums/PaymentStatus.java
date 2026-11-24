package com.transport.domain.enums;

// 결제 상태
public enum PaymentStatus {
	READY,      // 결제 대기 (신청서 저장 시 생성)
	PAID,       // 승인 완료
	CANCELED,   // 결제 취소 (환불)
	EXPIRED     // 30분 미결제로 만료
}
