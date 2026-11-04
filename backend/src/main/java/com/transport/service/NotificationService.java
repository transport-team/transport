package com.transport.service;

import com.transport.domain.Member;
import com.transport.domain.Notification;
import com.transport.domain.Shipment;
import com.transport.domain.enums.NotificationType;

// 알림 서비스 (팀원 4 — docs/collaboration.md 약속)
public interface NotificationService {

	/**
	 * 알림 전송
	 * @param receiver 수신자
	 * @param type 알림 종류
	 * @param message 알림 문구
	 * @param shipment 관련 배송 (없으면 null)
	 */
	void send(Member receiver, NotificationType type, String message, Shipment shipment);

	/**
	 * SSE 구독 등록
	 * @param memberId 회원 ID
	 * @param emitter SseEmitter
	 */
	void subscribe(Long memberId, org.springframework.web.servlet.mvc.method.annotation.SseEmitter emitter);
}