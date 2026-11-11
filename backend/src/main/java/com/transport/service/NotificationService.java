package com.transport.service;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.enums.NotificationType;
import com.transport.dto.NotificationResponseDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;

// 알림 서비스 (팀원 4 — docs/collaboration.md 약속)
public interface NotificationService {

	/**
	 * 알림 전송 (6-1: DB 저장 + 커밋 후 SSE 전송)
	 */
	void send(Member receiver, NotificationType type, String message, Shipment shipment);

	/**
	 * SSE 구독 등록
	 */
	void subscribe(Long memberId, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter);

	/**
	 * 내 알림 목록 (6-2) — 최신순 페이징
	 */
	PageResponseDTO<NotificationResponseDTO> getMyNotifications(Long memberId, PageRequestDTO pageRequestDTO);

	/**
	 * 안 읽은 알림 개수 (6-2)
	 */
	long getUnreadCount(Long memberId);

	/**
	 * 읽음 처리 (6-2) — 본인 알림만. 아니면 FORBIDDEN
	 */
	void markAsRead(Long notificationId, Long memberId);
}