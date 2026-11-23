package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 관리자 배차만 해제할 때 남길 사유 (주문 취소 및 환불은 별도 처리)
public record AdminDispatchCancelRequestDTO(
        @NotBlank(message = "배차 취소 사유를 입력해 주세요.")
        @Size(max = 180, message = "취소 사유는 180자 이내로 입력해 주세요.")
        String reason
) {}
