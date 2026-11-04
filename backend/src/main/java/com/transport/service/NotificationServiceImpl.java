package com.transport.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.transport.domain.Member;
import com.transport.domain.Notification;
import com.transport.domain.Shipment;
import com.transport.domain.enums.NotificationType;

import lombok.extern.log4j.Log4j2;

// 알림 서비스 구현 (팀원 4)
@Service
@Log4j2
public class NotificationServiceImpl implements NotificationService {

	// 회원별 SSE emitter 저장소
	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

	// Heartbeat 스케줄러 (30초마다)
	private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
		Thread t = new Thread(r, "notification-heartbeat");
		t.setDaemon(true);
		return t;
	});

	{
		heartbeatScheduler.scheduleAtFixedRate(this::sendHeartbeat, 30, 30, TimeUnit.SECONDS);
	}

	@Override
	public void send(Member receiver, NotificationType type, String message, Shipment shipment) {
		// TODO: DB 저장 + SSE 실시간 전송 (6주차에 구현)
		// 지금은 로그만
		log.info("[알림 전송] receiverId={}, type={}, message={}, shipmentId={}",
				receiver.getId(), type, message, shipment != null ? shipment.getId() : null);
	}

	@Override
	public void subscribe(Long memberId, SseEmitter emitter) {
		// 기존 연결 있으면 정리
		SseEmitter existing = emitters.put(memberId, emitter);
		if (existing != null) {
			try {
				existing.complete();
			} catch (Exception ignored) {}
		}

		// 연결 종료/타임아웃/에러 시 맵에서 제거
		emitter.onCompletion(() -> emitters.remove(memberId, emitter));
		emitter.onTimeout(() -> emitters.remove(memberId, emitter));
		emitter.onError(e -> emitters.remove(memberId, emitter));

		// 초기 연결 이벤트 전송 (브라우저가 연결됐음을 알림)
		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data("{\\\"connected\\\":true}"));
		} catch (IOException e) {
			emitters.remove(memberId, emitter);
			log.warn("SSE 초기 이벤트 전송 실패: memberId={}", memberId);
		}

		log.info("SSE 구독 등록: memberId={}, 현재 연결 수={}", memberId, emitters.size());
	}

	/** 30초마다 heartbeat 전송 */
	private void sendHeartbeat() {
		if (emitters.isEmpty()) return;

		emitters.forEach((memberId, emitter) -> {
			try {
				emitter.send(SseEmitter.event()
					.name("heartbeat")
					.data(""));
			} catch (IOException e) {
				emitters.remove(memberId, emitter);
				log.debug("Heartbeat 실패, 연결 제거: memberId={}", memberId);
			}
		});
	}
}