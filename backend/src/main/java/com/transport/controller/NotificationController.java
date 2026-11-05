package com.transport.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.transport.security.LoginMember;
import com.transport.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 알림 API (팀원 4)
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Log4j2
public class NotificationController {

	private final NotificationService notificationService;

	/**
	 * SSE 구독 — 알림 실시간 수신
	 * 프론트에서 EventSource로 이 엔드포인트 호출
	 * Authorization 헤더에 액세스 토큰 필요 (JWTCheckFilter가 검증 후 LoginMember 주입)
	 */
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal LoginMember loginMember) {
		Long memberId = loginMember.getId();
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // 타임아웃 없음 (heartbeat로 관리)

		notificationService.subscribe(memberId, emitter);

		log.info("SSE 구독 요청: memberId={}", memberId);
		return emitter;
	}
}