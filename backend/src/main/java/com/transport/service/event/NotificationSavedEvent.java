package com.transport.service.event;

import com.transport.domain.enums.NotificationType;

import java.time.LocalDateTime;

// 알림 저장 완료 이벤트 (6-1)
// send()가 DB 저장 후 발행 → AFTER_COMMIT 리스너(NotificationSseListener)가 커밋 성공 시에만 SSE 전송
// 롤백되면 알림 이벤트도 나가지 않는다 (저장 안 된 알림을 실시간으로 보여주는 문제 방지)
public record NotificationSavedEvent(
		Long notificationId,
		Long receiverId,
		NotificationType type,
		String message,
		Long shipmentId,
		LocalDateTime createdAt
) {
}