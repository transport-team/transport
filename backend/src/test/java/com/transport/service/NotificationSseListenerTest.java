package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.transport.domain.enums.NotificationType;
import com.transport.dto.NotificationResponseDTO;
import com.transport.service.event.NotificationSavedEvent;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// SSE 알림 본문 (6-1 후속): 목록 API 한 건과 같은 모양 + JSON 변환은 Jackson이 한다
class NotificationSseListenerTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	@DisplayName("createdAt · isRead가 들어가고, 따옴표 · 줄바꿈이 있는 메시지도 JSON이 깨지지 않는다")
	void payloadIsSafeJsonWithCreatedAt() {
		String message = "기사 \"홍길동\"님이 배정됐어요\n확인해 주세요";
		NotificationSavedEvent event = new NotificationSavedEvent(
				5L, 100L, NotificationType.DISPATCHED, message, 1L, LocalDateTime.of(2026, 11, 12, 9, 30));

		NotificationResponseDTO payload = NotificationSseListener.toPayload(event);
		String text = jsonMapper.writeValueAsString(payload);
		JsonNode json = jsonMapper.readTree(text);

		assertThat(text).doesNotContain("\n");  // SSE는 줄바꿈이 data 줄을 끊으므로 JSON 안에 진짜 줄바꿈이 없어야 한다
		assertThat(json.get("id").asLong()).isEqualTo(5L);
		assertThat(json.get("type").asString()).isEqualTo("DISPATCHED");
		assertThat(json.get("message").asString()).isEqualTo(message);
		assertThat(json.get("shipmentId").asLong()).isEqualTo(1L);
		assertThat(json.get("isRead").asBoolean()).isFalse();
		assertThat(json.get("createdAt").asString()).startsWith("2026-11-12T09:30");
	}

	@Test
	@DisplayName("관련 배송이 없는 알림은 shipmentId가 null")
	void shipmentIdCanBeNull() {
		NotificationSavedEvent event = new NotificationSavedEvent(
				6L, 100L, NotificationType.INQUIRY_ANSWERED, "문의에 답변이 달렸어요", null, LocalDateTime.of(2026, 11, 12, 9, 31));

		JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(NotificationSseListener.toPayload(event)));

		assertThat(json.get("shipmentId").isNull()).isTrue();
	}
}
