package com.transport.domain;

import com.transport.domain.enums.NotificationType;

import jakarta.persistence.*;
import lombok.*;

// 알림
@Entity
@Table(name = "notification")
@Getter
@ToString(exclude = {"receiver", "shipment"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notification extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "receiver_id", nullable = false)
	private Member receiver;  // 받는 회원

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id")
	private Shipment shipment;  // 관련 배송 (없으면 NULL)

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(30)")
	private NotificationType type;

	@Column(nullable = false)
	private String message;

	private boolean isRead;  // 읽음 여부 (READ는 SQL 예약어라 is_read)
}
