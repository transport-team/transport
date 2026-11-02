package com.transport.domain;

import jakarta.persistence.*;
import lombok.*;

// 차종 · 요금 규칙 (운임 계산 기준, docs/fare.md)
@Entity
@Table(name = "vehicle_type")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleType extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String code;  // SMALL · TON1 · TON2_5 · TON5

	@Column(nullable = false)
	private String name;  // 표시 이름

	private int maxLoadKg;  // 최대 적재량

	private int baseFare;  // 기본요금

	private int baseDistanceKm;  // 기본요금에 포함된 거리

	private int perKmFare;  // 초과 1km당 요금

	private int nightRate;  // 야간 할증률(%)

	private int weekendRate;  // 주말 할증률(%)

	private int heavyRate;  // 중량 할증률(%)

	private boolean active;
}
