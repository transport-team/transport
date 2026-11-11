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

// ???逾???類λ룴????뚮뿭寃?(????4)
// 6-1: send() = DB ????+ ???繹???꾩룇裕됵쭛? ???깆젷 SSE ?熬곣뫖苑?? NotificationSseListener?띠럾?
// ?筌뤾퍔???????ｋ걞???繹먭퍓沅???AFTER_COMMIT)????類ｋ펲 ???β뼯?뉐첎??濡?듆 ???逾?????嶺뚯솘? ???낅츎??
// 6-2: ?????逾?嶺뚮ㅄ維뽨빳???瑜곷턄嶺? 鸚?????? ?띠룇裕??鸚?????쾳 嶺뚳퐣瑗???곌랜梨??우춹?
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

		log.info("[???逾????? id={}, receiverId={}, type={}, shipmentId={}",
				saved.getId(), receiver.getId(), type, shipment != null ? shipment.getId() : null);
	}

	/** SSE ?洹먮봾裕???깅뮔 ?????????熬곣뫗??emitter (??怨몃さ嶺?null) */
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
			throw new CustomException(ErrorCode.FORBIDDEN, "?곌랜梨??????逾앯춯?????쾳 嶺뚳퐣瑗????????곕????덈펲.");
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
			log.warn("SSE ?貫?껆뵳????繹???熬곣뫖苑????덉넮: memberId={}", memberId);
		}

		log.info("SSE ??뚮맧利??繹먮굞夷? memberId={}, ?熬곣뫗????⑤슡????{}", memberId, emitters.size());
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
				log.debug("Heartbeat ???덉넮, ??⑤슡????蹂ㅽ깴: memberId={}", memberId);
			}
		});
	}
}