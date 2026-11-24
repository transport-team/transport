package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

import com.transport.repository.NotificationRepository;

// SSE 여러 탭 연결 (#60 · #94): 새 탭이 이전 탭을 끊지 않고, 알림 · 위치는 그 회원의 모든 탭에 간다
// 끊긴 탭만 빠지고, heartbeat는 어떤 예외에도 멈추지 않는다 (DB 없이, SseEmitter는 가짜)
class NotificationServiceImplTest {

	private NotificationServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new NotificationServiceImpl(mock(NotificationRepository.class), mock(ApplicationEventPublisher.class));
	}

	private SseEmitter tab() {
		return mock(SseEmitter.class);
	}

	@Test
	@DisplayName("같은 회원이 탭 2개로 구독하면 둘 다 연결이 남고, 이벤트는 두 탭 모두에 간다")
	void twoTabsBothReceive() throws IOException {
		SseEmitter first = tab();
		SseEmitter second = tab();

		service.subscribe(1L, first);
		service.subscribe(1L, second);
		int sent = service.sendToMember(1L, "location", "{\"lat\":37.5}");

		assertThat(sent).isEqualTo(2);
		assertThat(service.connectionCount(1L)).isEqualTo(2);
		verify(first, never()).complete();                           // 새 탭이 이전 탭을 끊지 않는다
		verify(first, times(2)).send(any(SseEventBuilder.class));    // connected + location
		verify(second, times(2)).send(any(SseEventBuilder.class));
	}

	@Test
	@DisplayName("끊긴 탭(IOException)만 빠지고 다른 탭은 계속 받는다")
	void brokenTabIsRemovedOthersKeepReceiving() throws IOException {
		SseEmitter broken = tab();
		SseEmitter alive = tab();
		doNothing().doThrow(new IOException("끊김")).when(broken).send(any(SseEventBuilder.class));
		service.subscribe(1L, broken);
		service.subscribe(1L, alive);

		assertThat(service.sendToMember(1L, "notification", "a")).isEqualTo(1);
		assertThat(service.connectionCount(1L)).isEqualTo(1);
		assertThat(service.sendToMember(1L, "notification", "b")).isEqualTo(1);
		verify(broken, times(2)).send(any(SseEventBuilder.class));   // connected + 실패 1번 — 그 뒤로는 안 보낸다
		verify(alive, times(3)).send(any(SseEventBuilder.class));
	}

	@Test
	@DisplayName("이미 끝난 연결의 IllegalStateException도 밖으로 나가지 않고 그 연결만 빠진다")
	void completedTabIllegalStateIsHandled() throws IOException {
		SseEmitter done = tab();
		doNothing().doThrow(new IllegalStateException("ResponseBodyEmitter has already completed"))
				.when(done).send(any(SseEventBuilder.class));
		service.subscribe(1L, done);

		assertThatCode(() -> service.sendToMember(1L, "location", "x")).doesNotThrowAnyException();
		assertThat(service.connectionCount(1L)).isZero();
	}

	@Test
	@DisplayName("탭이 닫히면(onCompletion) 그 연결만 빠진다")
	void completionRemovesOnlyThatTab() {
		SseEmitter closing = tab();
		SseEmitter staying = tab();
		service.subscribe(1L, closing);
		service.subscribe(1L, staying);

		ArgumentCaptor<Runnable> onCompletion = ArgumentCaptor.forClass(Runnable.class);
		verify(closing).onCompletion(onCompletion.capture());
		onCompletion.getValue().run();

		assertThat(service.connectionCount(1L)).isEqualTo(1);
		assertThat(service.sendToMember(1L, "notification", "x")).isEqualTo(1);
	}

	@Test
	@DisplayName("회원당 연결이 최대 수를 넘으면 가장 오래된 탭부터 닫는다")
	void oldestTabIsClosedOverLimit() {
		List<SseEmitter> tabs = new ArrayList<>();
		for (int i = 0; i <= NotificationServiceImpl.MAX_CONNECTIONS_PER_MEMBER; i++) {
			SseEmitter emitter = tab();
			tabs.add(emitter);
			service.subscribe(1L, emitter);
		}

		verify(tabs.get(0)).complete();
		verify(tabs.get(1), never()).complete();
		assertThat(service.connectionCount(1L)).isEqualTo(NotificationServiceImpl.MAX_CONNECTIONS_PER_MEMBER);
	}

	@Test
	@DisplayName("heartbeat는 한 연결이 예외를 내도 멈추지 않고, 그 연결만 정리한다")
	void heartbeatSurvivesBrokenConnections() throws IOException {
		SseEmitter broken = tab();
		SseEmitter other = tab();
		doNothing().doThrow(new IllegalStateException("끝난 연결")).when(broken).send(any(SseEventBuilder.class));
		service.subscribe(1L, broken);
		service.subscribe(2L, other);

		assertThatCode(service::sendHeartbeat).doesNotThrowAnyException();

		assertThat(service.connectionCount(1L)).isZero();
		assertThat(service.connectionCount(2L)).isEqualTo(1);
		verify(other, times(2)).send(any(SseEventBuilder.class));   // connected + heartbeat
	}

	@Test
	@DisplayName("연결이 없는 회원에게는 보내지 않고 0을 돌려준다 (알림은 DB 저장분만)")
	void noConnectionReturnsZero() {
		assertThat(service.sendToMember(99L, "notification", "x")).isZero();
	}
}
