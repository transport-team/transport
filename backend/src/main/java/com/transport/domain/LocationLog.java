package com.transport.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

// 기사 위치 기록 (IN_TRANSIT 상태에서만 저장, 30일 후 삭제)
@Entity
@Table(name = "location_log", indexes = @Index(
		name = "idx_location_log_shipment_recorded", columnList = "shipment_id, recorded_at"))
@Getter
@ToString(exclude = {"shipment", "driver"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LocationLog extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id", nullable = false)
	private Driver driver;

	private double lat;

	private double lng;

	@Column(nullable = false)
	private LocalDateTime recordedAt;  // 측정 시각
}
