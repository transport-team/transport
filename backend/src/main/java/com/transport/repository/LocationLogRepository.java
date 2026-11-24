package com.transport.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.LocationLog;

// 위치 기록 (7-1)
public interface LocationLogRepository extends JpaRepository<LocationLog, Long> {

	// 마지막 위치 1건 (고객 상세 지도 초기 위치)
	Optional<LocationLog> findTopByShipmentIdOrderByRecordedAtDesc(Long shipmentId);

	// 30일 지난 기록 삭제 (LocationCleanupScheduler — 7-4)
	long deleteByRecordedAtBefore(java.time.LocalDateTime cutoff);
}