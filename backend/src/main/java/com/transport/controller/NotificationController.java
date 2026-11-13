package com.transport.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import com.transport.dto.NotificationResponseDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.transport.security.LoginMember;
import com.transport.service.NotificationService;

import jakarta.validation.Valid;
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

	// ===== 6-2 알림 목록 · 읽음 =====

	/** 내 알림 목록 (페이지) + 안 읽은 개수 — 본인 것만 */
	@GetMapping
	public Map<String, Object> getMyNotifications(
			@AuthenticationPrincipal LoginMember loginMember,
			@Valid PageRequestDTO pageRequestDTO) { // page ≥ 1 · size 1~100 (다른 목록 API와 같은 검사 — 틀리면 400)

		PageResponseDTO<NotificationResponseDTO> dto =
				notificationService.getMyNotifications(loginMember.getId(), pageRequestDTO);
		long unreadCount = notificationService.getUnreadCount(loginMember.getId());

		return Map.of(
				"page", dto,
				"unreadCount", unreadCount);
	}

	/** 알림 읽음 처리 — 본인 알림만 (아니면 403 FORBIDDEN) */
	@PatchMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(
			@AuthenticationPrincipal LoginMember loginMember,
			@PathVariable Long id) {

		notificationService.markAsRead(id, loginMember.getId());
		return ResponseEntity.noContent().build();
	}
}
