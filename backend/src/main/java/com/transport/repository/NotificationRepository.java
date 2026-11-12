package com.transport.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Notification;
import com.transport.domain.Member;

// 알림 (팀원 4)
public interface NotificationRepository extends JpaRepository<Notification, Long> {

	// 회원별 알림 목록 (최신순, 페이징)
	@EntityGraph(attributePaths = "shipment")
	Page<Notification> findByReceiverOrderByCreatedAtDesc(Member receiver, Pageable pageable);

	// 안 읽은 개수
	long countByReceiverAndIsReadFalse(Member receiver);

	// 본인 알림만 조회 (읽음 처리 시 보안 체크용)
	Notification findByIdAndReceiver(Long id, Member receiver);
}