package com.transport.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.transport.dto.NotificationResponseDTO;
import com.transport.service.event.NotificationSavedEvent;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 알림 SSE 전송 리스너 (6-1)
// AFTER_COMMIT: send()를 호출한 트랜잭션이 '커밋 성공'했을 때만 실행된다.
//  - 롤백되면(예: 상태 변경이 예외로 무효) 알림도 나가지 않는다
//  - 커밋 전에 보내면 클라이언트가 알림을 받고 상세를 조회했는데 데이터가 아직 없는 상황이 생긴다
@Component
@RequiredArgsConstructor
@Log4j2
public class NotificationSseListener {

	private final NotificationServiceImpl notificationService; // subscribe/emitter 저장소 접근

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(NotificationSavedEvent event) {
		SseEmitter emitter = notificationService.getEmitter(event.receiverId());
		if (emitter == null) {
			log.debug("SSE 미접속 수신자 — DB 저장분만 유효: receiverId={}", event.receiverId());
			return;
		}
		try {
			emitter.send(SseEmitter.event()
					.name("notification")
					.data(toPayload(event), MediaType.APPLICATION_JSON));
			log.info("SSE 알림 전송: receiverId={}, type={}", event.receiverId(), event.type());
		} catch (Exception e) {
			log.warn("SSE 알림 전송 실패(연결 정리 필요): receiverId={}", event.receiverId());
		}
	}

	// 목록 API(GET /api/notifications)의 한 건과 같은 모양 — JSON 변환은 Spring이 한다
	// (손으로 JSON을 만들면 createdAt이 빠지거나 메시지의 따옴표 때문에 JSON이 깨진다)
	static NotificationResponseDTO toPayload(NotificationSavedEvent event) {
		return NotificationResponseDTO.builder()
				.id(event.notificationId())
				.type(event.type())
				.message(event.message())
				.shipmentId(event.shipmentId())
				.read(false)
				.createdAt(event.createdAt())
				.build();
	}
}