package com.transport.dto;

import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

// 기사 근무 가능 시간 등록 · 수정 요청 (DRV-01) — 끝이 시작보다 이르면 자정을 넘기는 근무
public record DriverWorkHoursRequestDTO(
        @NotNull(message = "시작 시간을 입력해 주세요.")
        LocalTime startTime,

        @NotNull(message = "끝 시간을 입력해 주세요.")
        LocalTime endTime
) {
}
