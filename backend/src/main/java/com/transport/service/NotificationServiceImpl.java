package com.transport.service;

import java.time.LocalDateTime;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
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
// SSE 연결은 회원당 여러 개(탭마다 하나)를 들고 있고, 알림 · 위치는 그 회원의 모든 탭에 보낸다
// (예전에는 회원당 하나라 새 탭이 이전 탭을 끊었다 — #60 · #94, 팀원 4 휴가로 팀원 1이 대신)
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class NotificationServiceImpl implements NotificationService {

	// 회원당 최대 연결 수 — 넘으면 가장 오래된 탭을 닫는다 (닫힌 탭은 polyfill이 다시 붙는다)
	static final int MAX_CONNECTIONS_PER_MEMBER = 5;

	// 회원 ID → 그 회원의 SSE 연결들 (오래된 것이 앞)
	private final Map<Long, Deque<SseEmitter>> emitters = new ConcurrentHashMap<>();
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

	/**
	 * 그 회원의 모든 SSE 연결(탭)에 이벤트를 보낸다 — 알림 리스너 · 위치 서비스 · heartbeat가 쓴다.
	 * 실패한 연결(닫힌 탭)만 빼고 나머지 탭은 계속 받는다. 보낸 연결 수를 돌려준다 (연결이 없으면 0).
	 */
	public int sendToMember(Long memberId, String eventName, Object data, MediaType mediaType) {
		Deque<SseEmitter> connections = emitters.get(memberId);
		if (connections == null || connections.isEmpty()) {
			return 0;
		}
		int sent = 0;
		for (SseEmitter emitter : connections) {
			try {
				SseEmitter.SseEventBuilder event = SseEmitter.event().name(eventName);
				emitter.send(mediaType != null ? event.data(data, mediaType) : event.data(data));
				sent++;
			} catch (Exception e) {
				// IOException(끊긴 탭)뿐 아니라 이미 끝난 연결의 IllegalStateException도 여기서 정리한다
				removeEmitter(memberId, emitter);
				log.debug("SSE 전송 실패 → 그 연결만 제거: memberId={}, event={}", memberId, eventName);
			}
		}
		return sent;
	}

	public int sendToMember(Long memberId, String eventName, Object data) {
		return sendToMember(memberId, eventName, data, null);
	}

	/** 그 회원의 현재 SSE 연결 수 (시험 · 로그용) */
	int connectionCount(Long memberId) {
		Deque<SseEmitter> connections = emitters.get(memberId);
		return connections == null ? 0 : connections.size();
	}

	// 연결 하나만 빼고, 그 회원의 연결이 다 없어지면 키도 지운다
	private void removeEmitter(Long memberId, SseEmitter emitter) {
		emitters.computeIfPresent(memberId, (id, connections) -> {
			connections.remove(emitter);
			return connections.isEmpty() ? null : connections;
		});
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
		// 새 탭은 연결을 하나 더한다 (이전 탭은 그대로 둔다)
		Deque<SseEmitter> connections = emitters.computeIfAbsent(memberId, id -> new ConcurrentLinkedDeque<>());
		connections.addLast(emitter);

		// 너무 많으면 가장 오래된 탭부터 닫는다 (새로고침을 거듭해 남은 연결이 쌓이지 않게)
		while (connections.size() > MAX_CONNECTIONS_PER_MEMBER) {
			SseEmitter oldest = connections.pollFirst();
			if (oldest == null) {
				break;
			}
			try {
				oldest.complete();
			} catch (Exception ignored) {}
		}

		emitter.onCompletion(() -> removeEmitter(memberId, emitter));
		emitter.onTimeout(() -> removeEmitter(memberId, emitter));
		emitter.onError(e -> removeEmitter(memberId, emitter));

		try {
			emitter.send(SseEmitter.event()
				.name("connected")
				.data("{\"connected\":true}"));
		} catch (Exception e) {
			removeEmitter(memberId, emitter);
			log.warn("SSE 초기 이벤트 전송 실패: memberId={}", memberId);
		}

		log.info("SSE 구독 등록: memberId={}, 이 회원 연결 수={}, 연결된 회원 수={}",
				memberId, connectionCount(memberId), emitters.size());
	}

	// 30초마다 모든 연결에 heartbeat — 끊긴 연결을 찾아 정리한다
	// scheduleAtFixedRate는 작업에서 예외가 한 번이라도 나가면 다음부터 멈추므로 어떤 예외도 밖으로 내보내지 않는다
	void sendHeartbeat() {
		try {
			emitters.keySet().forEach(memberId -> sendToMember(memberId, "heartbeat", ""));
		} catch (Exception e) {
			log.warn("Heartbeat 중 오류 (다음 주기는 계속): {}", e.getMessage());
		}
	}
}