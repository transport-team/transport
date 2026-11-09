package com.transport.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.DriverSchedule;

public interface DriverScheduleRepository extends JpaRepository<DriverSchedule, Long> {

	// 기사 ID와 날짜로 근무 일정 조회
	Optional<DriverSchedule> findByDriver_IdAndWorkDate(Long driverId, LocalDate workDate);

}
