package com.transport.domain;

import java.time.LocalDate;
import java.time.LocalTime;

import com.transport.domain.enums.DriverScheduleStatus;

import jakarta.persistence.*;
import lombok.*;

// 기사 근무 일정 (기사 + 날짜 1건)
@Entity
@Table(name = "driver_schedule", uniqueConstraints = @UniqueConstraint(
		name = "uk_driver_schedule_driver_date", columnNames = {"driver_id", "work_date"}))
@Getter
@ToString(exclude = "driver")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DriverSchedule extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id", nullable = false)
	private Driver driver;

	@Column(nullable = false)
	private LocalDate workDate;

	@Column(nullable = false)
	private LocalTime startTime;  // 근무 가능 시작

	@Column(nullable = false)
	private LocalTime endTime;  // 근무 가능 끝

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private DriverScheduleStatus status;
}
