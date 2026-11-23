package com.transport.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.transport.domain.DriverSchedule;

// 기사 근무 가능 시간 (날짜별, DRV-01) — overnight: 끝이 시작보다 일러 다음 날로 넘어가는 근무
public record DriverWorkHoursResponseDTO(
        LocalDate date,
        LocalTime startTime,
        LocalTime endTime,
        boolean overnight
) {
    public static DriverWorkHoursResponseDTO from(DriverSchedule schedule) {
        return new DriverWorkHoursResponseDTO(
                schedule.getWorkDate(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getEndTime().isBefore(schedule.getStartTime()));
    }
}
