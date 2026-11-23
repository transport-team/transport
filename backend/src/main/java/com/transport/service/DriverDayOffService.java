package com.transport.service;

import java.time.LocalDate;
import java.util.List;

public interface DriverDayOffService {

    List<LocalDate> getOffDays(Long memberId, int year, int month);

    void registerOffDay(Long memberId, LocalDate date);

    void cancelOffDay(Long memberId, LocalDate date);
}
