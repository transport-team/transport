package com.transport.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.transport.repository.LocationLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 위치 기록 정리 (7-4 — 팀원 4)
// erd.md: location_log는 30일이 지나면 삭제 대상. 매일 새벽 4시에 실행한다.
// 위치는 개인정보라 오래 남길 이유가 없다 (운송 중 실시간 추적에만 쓰는 데이터).
@Component
@RequiredArgsConstructor
@Transactional
@Log4j2
public class LocationCleanupScheduler {

	private final LocationLogRepository locationLogRepository;

	@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")  // 매일 새벽 4시 (KST)
	public void cleanupOldLocations() {
		LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
		long deleted = locationLogRepository.deleteByRecordedAtBefore(cutoff);
		if (deleted > 0) {
			log.info("위치 기록 정리 완료: {}건 삭제 (기준 {} 이전)", deleted, cutoff.toLocalDate());
		}
	}
}