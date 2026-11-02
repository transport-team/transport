package com.transport.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

// 배차 (재배차해도 이력을 남기고, 현재 배차는 active = true 1건)
@Entity
@Table(name = "dispatch")
@Getter
@ToString(exclude = {"shipment", "driver", "vehicle", "assignedBy"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Dispatch extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id", nullable = false)
	private Driver driver;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assigned_by", nullable = false)
	private Member assignedBy;  // 배차한 관리자

	private boolean active;  // 현재 유효 배차

	@Column(nullable = false)
	private LocalDateTime assignedAt;

	private LocalDateTime releasedAt;  // 배차 해제 시각
}
