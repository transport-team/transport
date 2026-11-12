package com.transport.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.transport.domain.enums.NotificationType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// 알림 JSON 키 (6-2 후속): 화면(NotificationBell · useNotification)은 n.isRead를 읽는다
class NotificationResponseDTOTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	@DisplayName("읽음 여부는 isRead 키 하나로만 나간다 (read 키가 따로 생기지 않는다)")
	void readFlagUsesIsReadKey() {
		NotificationResponseDTO dto = NotificationResponseDTO.builder()
				.id(1L)
				.type(NotificationType.STATUS_CHANGED)
				.message("운송이 시작됐어요")
				.read(true)
				.build();

		JsonNode json = jsonMapper.readTree(jsonMapper.writeValueAsString(dto));

		assertThat(json.get("isRead").asBoolean()).isTrue();
		assertThat(json.has("read")).isFalse();
	}
}
