package com.transport.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.transport.dto.DriverWorkHoursResponseDTO;

public interface DriverDayOffService {

    List<LocalDate> getOffDays(Long memberId, int year, int month);

    void registerOffDay(Long memberId, LocalDate date);

    void cancelOffDay(Long memberId, LocalDate date);

    // ===== 근무 가능 시간 (DRV-01) — 같은 날짜에 휴무와 근무 시간은 하나만 =====

    // 월별 근무 가능 시간 (날짜 순)
    List<DriverWorkHoursResponseDTO> getWorkHours(Long memberId, int year, int month);

    // 근무 가능 시간 등록 · 수정 (오늘 이후, 휴무일이면 거절, 끝 < 시작이면 자정을 넘기는 근무)
    DriverWorkHoursResponseDTO saveWorkHours(Long memberId, LocalDate date, LocalTime startTime, LocalTime endTime);

    // 근무 가능 시간 삭제 (휴무일은 cancelOffDay)
    void deleteWorkHours(Long memberId, LocalDate date);
}
