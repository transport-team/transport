package com.transport.dto;

import java.time.LocalDate;

// 기사 휴무일 등록 요청
public record DriverDayOffRequestDTO(
        LocalDate date
) {
}