package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PaymentCancelRequestDTO {

    @NotBlank(message = "취소 사유를 입력해 주세요.")
    @Size(max = 500, message = "취소 사유는 500자 이내로 입력해 주세요.")
    private String cancelReason;
}