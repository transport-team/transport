package com.transport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

// 기사 배차 지원 요청 정보
@Getter
@Setter
public class DispatchApplyRequestDTO {

    @NotNull(message = "지원할 차량을 선택해 주세요.")
    @Positive(message = "차량 정보가 올바르지 않습니다.")
    private Long vehicleId;

}
