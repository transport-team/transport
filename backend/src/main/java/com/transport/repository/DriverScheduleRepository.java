package com.transport.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.DriverSchedule;

public interface DriverScheduleRepository extends JpaRepository<DriverSchedule, Long> {

    // 기사 ID와 날짜로 근무 일정 조회
    Optional<DriverSchedule> findByDriver_IdAndWorkDate(Long driverId, LocalDate workDate);

    // 기사의 특정 기간 근무 일정 조회
    List<DriverSchedule> findByDriver_IdAndWorkDateBetweenOrderByWorkDateAsc(
            Long driverId, LocalDate startDate, LocalDate endDate);

}