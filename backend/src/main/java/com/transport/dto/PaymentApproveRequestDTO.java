package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class PaymentApproveRequestDTO {

    @NotBlank(message = "주문번호는 필수입니다.")
    private String orderId;

    @NotNull(message = "결제 금액은 필수입니다.")
    private Integer amount;

    @NotBlank(message = "결제 키는 필수입니다.")
    private String paymentKey;
}