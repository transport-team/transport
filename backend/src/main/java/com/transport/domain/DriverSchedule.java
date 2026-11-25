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

	// 특정 날짜 휴무와 근무시간 예외를 한 번의 DB 변경으로 전환한다.
	public void changeToOff() {
		this.status = DriverScheduleStatus.OFF;
		this.startTime = LocalTime.MIN;
		this.endTime = LocalTime.MAX;
	}

	public void changeToAvailable(LocalTime startTime, LocalTime endTime) {
		this.status = DriverScheduleStatus.AVAILABLE;
		changeWorkHours(startTime, endTime);
	}

	// 근무 가능 시간 바꾸기 (DRV-01 — 근무 가능 일정만 쓴다. 휴무는 시간을 쓰지 않는다)
	public void changeWorkHours(LocalTime startTime, LocalTime endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}
}
