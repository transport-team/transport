package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.Notification;
import com.transport.domain.enums.NotificationType;

import lombok.Builder;
import lombok.Getter;

// 알림 한 건 (6-2 — GET /api/notifications 목록 · SSE 이벤트용)
@Getter
@Builder
public class NotificationResponseDTO {
	private Long id;
	private NotificationType type;
	private String message;
	private Long shipmentId;   // 관련 배송 (없으면 null — 예: 문의 답변)
	private boolean isRead;
	private LocalDateTime createdAt;

	public static NotificationResponseDTO from(Notification n) {
		return NotificationResponseDTO.builder()
				.id(n.getId())
				.type(n.getType())
				.message(n.getMessage())
				.shipmentId(n.getShipment() != null ? n.getShipment().getId() : null)
				.isRead(n.isRead())
				.createdAt(n.getCreatedAt())
				.build();
	}
}