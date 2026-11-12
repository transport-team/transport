package com.transport.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.transport.domain.Member;
import com.transport.domain.Notification;
import com.transport.domain.Shipment;
import com.transport.domain.enums.NotificationType;
import com.transport.dto.NotificationResponseDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.NotificationRepository;
import com.transport.service.event.NotificationSavedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 알림 서비스 구현 (팀원 4)
// 6-1: send() = DB 저장 + 저장 완료 이벤트 발행 → 실제 SSE 전송은 NotificationSseListener가 한다
// 커밋이 성공한 뒤(AFTER_COMMIT)에만 보낸다 — 롤백되면 저장되지 않은 알림이 실시간으로 나가지 않는다
// 6-2: 내 알림 목록 (페이지) · 안 읽은 개수 · 읽음 처리 (본인 알림만)
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class NotificationServiceImpl implements NotificationService {

	private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();
	private final NotificationRepository notificationRepository;
	private final ApplicationEventPublisher eventPublisher;

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
		Notification saved = notificationRepository.save(Notification.builder()
				.receiver(receiver)
				.type(type)
				.message(message)
				.shipment(shipment)
				.isRead(false)
				.build());

		eventPublisher.publishEvent(new NotificationSavedEvent(
				saved.getId(), receiver.getId(), type, message,
				shipment != null ? shipment.getId() : null,
				LocalDateTime.now()));

		log.info("[알림 저장] id={}, receiverId={}, type={}, shipmentId={}",
				saved.getId(), receiver.getId(), type, shipment != null ? shipment.getId() : null);
	}

	/** SSE 리스너가 쓰는 회원별 현재 emitter (없으면 null) */
	public SseEmitter getEmitter(Long memberId) {
		return emitters.get(memberId);
	}

	@Override
	@Transactional(readOnly = true)
	public PageResponseDTO<NotificationResponseDTO> getMyNotifications(Long memberId, PageRequestDTO pageRequestDTO) {
		Member receiver = Member.builder().id(memberId).build();
		Pageable pageable = pageRequestDTO.getPageable(Sort.by("createdAt").descending());
		Page<Notification> page = notificationRepository.findByReceiverOrderByCreatedAtDesc(receiver, pageable);

		return new PageResponseDTO<>(
				page.getContent().stream().map(NotificationResponseDTO::from).toList(),
				pageRequestDTO, page.getTotalElements());
	}

	@Override
	@Transactional(readOnly = true)
	public long getUnreadCount(Long memberId) {
		Member receiver = Member.builder().id(memberId).build();
		return notificationRepository.countByReceiverAndIsReadFalse(receiver);
	}

	@Override
	public void markAsRead(Long notificationId, Long memberId) {
		Notification notification = notificationRepository.findByIdAndReceiver(notificationId,
				Member.builder().id(memberId).build());
		if (notification == null) {
			throw new CustomException(ErrorCode.FORBIDDEN, "본인 알림만 읽음 처리할 수 있습니다.");
		}
		if (!notification.isRead()) {
			notification.markAsRead();
		}
	}

	@Override
	public void subscribe(Long memberId, SseEmitter emitter) {
		SseEmitter existing = emitters.put(memberId, emitter);
		if (existing != null) {
			try {
				existing.complete();
			} catch (Exception ignored) {}
		}

		emitter.onCompletion(() -> emitters.remove(memberId, emitter));
		emitter.onTimeout(() -> emitters.remove(memberId, emitter));
		emitter.onError(e -> emitters.remove(memberId, emitter));

		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data("{\"connected\":true}"));
		} catch (IOException e) {
			emitters.remove(memberId, emitter);
			log.warn("SSE 초기 이벤트 전송 실패: memberId={}", memberId);
		}

		log.info("SSE 구독 등록: memberId={}, 현재 연결 수={}", memberId, emitters.size());
	}

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