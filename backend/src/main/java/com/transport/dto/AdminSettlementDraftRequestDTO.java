package com.transport.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

// 수수료율은 정책이 정해지지 않았으므로 관리자가 반드시 직접 입력한다.
public record AdminSettlementDraftRequestDTO(
        @NotBlank @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])") String month,
        @NotNull @Min(0) @Max(100) Integer commissionRate) {
}
