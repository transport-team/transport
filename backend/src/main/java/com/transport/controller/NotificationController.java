package com.transport.controller;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import java.util.Map;
import com.transport.dto.NotificationResponseDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.transport.security.LoginMember;
import com.transport.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// ???뵝 API (????4)
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Log4j2
public class NotificationController {

	private final NotificationService notificationService;

	/**
	 * SSE ?닌됰즴 ?????뵝 ??쇰뻻揶???뤿뻿
	 * ?袁⑥쨴?紐꾨퓠??EventSource嚥????遺얜굡??????紐꾪뀱
	 * Authorization ??삳쐭????り쉭???醫뤾쿃 ?袁⑹뒄 (JWTCheckFilter揶쎛 野꺜筌???LoginMember 雅뚯눘??
	 */
	@GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter subscribe(@AuthenticationPrincipal LoginMember loginMember) {
		Long memberId = loginMember.getId();
		SseEmitter emitter = new SseEmitter(Long.MAX_VALUE); // ???袁⑸툡????곸벉 (heartbeat嚥??온??

		notificationService.subscribe(memberId, emitter);

		log.info("SSE ?닌됰즴 ?遺욧퍕: memberId={}", memberId);
		return emitter;
	}

	// ===== 6-2 ?뚮┝ 紐⑸줉 쨌 ?쎌쓬 =====

	/** ???뚮┝ 紐⑸줉 (?섏씠吏? + ???쎌? 媛쒖닔 ??蹂몄씤 寃껊쭔 */
	@GetMapping
	public Map<String, Object> getMyNotifications(
			@AuthenticationPrincipal LoginMember loginMember,
			@RequestParam(defaultValue = "1") int page,
			@RequestParam(defaultValue = "10") int size) {

		PageRequestDTO pageRequestDTO = PageRequestDTO.builder().page(page).size(size).build();
		PageResponseDTO<NotificationResponseDTO> dto =
				notificationService.getMyNotifications(loginMember.getId(), pageRequestDTO);
		long unreadCount = notificationService.getUnreadCount(loginMember.getId());

		return Map.of(
				"page", dto,
				"unreadCount", unreadCount);
	}

	/** ?뚮┝ ?쎌쓬 泥섎━ ??蹂몄씤 ?뚮┝留?(?꾨땲硫?403 FORBIDDEN) */
	@PatchMapping("/{id}/read")
	public ResponseEntity<Void> markAsRead(
			@AuthenticationPrincipal LoginMember loginMember,
			@PathVariable Long id) {

		notificationService.markAsRead(id, loginMember.getId());
		return ResponseEntity.noContent().build();
	}}
