package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 관리자 차량 등록 신청 거절 입력값
@Getter
@Setter
public class VehicleRegistrationRejectDTO {

    @NotBlank(message = "거절 사유를 입력해 주세요.")
    @Size(max = 300, message = "거절 사유는 300자 이내로 입력해 주세요.")
    private String rejectionReason;

}