package com.transport.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import com.transport.domain.DriverSchedule;
import com.transport.domain.enums.DriverScheduleStatus;

// 날짜별 예외가 없으면 월~토 09:00~18:00, 일요일 휴무를 적용한다.
public final class DriverWorkSchedulePolicy {
    public static final LocalTime DEFAULT_START = LocalTime.of(9, 0);
    public static final LocalTime DEFAULT_END = LocalTime.of(18, 0);

    private DriverWorkSchedulePolicy() {
    }

    public static boolean isOff(LocalDate date, Optional<DriverSchedule> override) {
        if (override != null && override.isPresent()) {
            return override.get().getStatus() == DriverScheduleStatus.OFF;
        }
        return date.getDayOfWeek() == DayOfWeek.SUNDAY;
    }

    public static LocalTime start(Optional<DriverSchedule> override) {
        return override != null && override.isPresent()
                ? override.get().getStartTime() : DEFAULT_START;
    }

    public static LocalTime end(Optional<DriverSchedule> override) {
        return override != null && override.isPresent()
                ? override.get().getEndTime() : DEFAULT_END;
    }
}
