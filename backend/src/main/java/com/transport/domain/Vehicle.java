package com.transport.domain;

import com.transport.domain.enums.VehicleStatus;

import jakarta.persistence.*;
import lombok.*;

// 차량
@Entity
@Table(name = "vehicle")
@Getter
@ToString(exclude = {"vehicleType", "driver"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Vehicle extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 20)
	private String plateNo;  // 차량번호

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_type_id", nullable = false)
	private VehicleType vehicleType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private VehicleStatus status;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id", unique = true)
	private Driver driver;  // 담당 기사 (없으면 NULL)
}
