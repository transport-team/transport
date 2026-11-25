package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.transport.domain.DriverSchedule;
import com.transport.domain.enums.DriverScheduleStatus;

class DriverWorkSchedulePolicyTest {
    private final LocalDate sunday = LocalDate.of(2026, 11, 29);
    private final LocalDate monday = sunday.plusDays(1);

    @Test
    void defaultsToMondayThroughSaturdayAndSundayOff() {
        assertThat(DriverWorkSchedulePolicy.isOff(sunday, Optional.empty())).isTrue();
        assertThat(DriverWorkSchedulePolicy.isOff(monday, Optional.empty())).isFalse();
        assertThat(DriverWorkSchedulePolicy.start(Optional.empty())).isEqualTo(LocalTime.of(9, 0));
        assertThat(DriverWorkSchedulePolicy.end(Optional.empty())).isEqualTo(LocalTime.of(18, 0));
    }

    @Test
    void workOverrideAllowsSundayAndOffOverrideBlocksWeekday() {
        DriverSchedule sundayWork = DriverSchedule.builder().workDate(sunday)
                .status(DriverScheduleStatus.AVAILABLE)
                .startTime(LocalTime.of(13, 0)).endTime(LocalTime.of(20, 0)).build();
        DriverSchedule mondayOff = DriverSchedule.builder().workDate(monday)
                .status(DriverScheduleStatus.OFF)
                .startTime(LocalTime.MIN).endTime(LocalTime.MAX).build();

        assertThat(DriverWorkSchedulePolicy.isOff(sunday, Optional.of(sundayWork))).isFalse();
        assertThat(DriverWorkSchedulePolicy.start(Optional.of(sundayWork))).isEqualTo(LocalTime.of(13, 0));
        assertThat(DriverWorkSchedulePolicy.end(Optional.of(sundayWork))).isEqualTo(LocalTime.of(20, 0));
        assertThat(DriverWorkSchedulePolicy.isOff(monday, Optional.of(mondayOff))).isTrue();
    }
}
