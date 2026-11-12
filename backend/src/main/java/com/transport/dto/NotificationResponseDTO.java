package com.transport.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;
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

	// JSON 키는 isRead (화면이 n.isRead로 읽는다)
	// 필드 이름을 isRead로 두면 Lombok 게터가 isRead()가 되어 Jackson이 키를 "read"로 바꾼다 → 필드는 read, 키는 직접 적는다
	@JsonProperty("isRead")
	private boolean read;

	private LocalDateTime createdAt;

	public static NotificationResponseDTO from(Notification n) {
		return NotificationResponseDTO.builder()
				.id(n.getId())
				.type(n.getType())
				.message(n.getMessage())
				.shipmentId(n.getShipment() != null ? n.getShipment().getId() : null)
				.read(n.isRead())
				.createdAt(n.getCreatedAt())
				.build();
	}
}