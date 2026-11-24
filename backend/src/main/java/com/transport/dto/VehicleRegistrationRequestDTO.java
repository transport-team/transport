package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

// 기사 차량 등록 신청 입력값
@Getter
@Setter
public class VehicleRegistrationRequestDTO {

    @NotBlank(message = "차량번호를 입력해 주세요.")
    @Size(max = 20, message = "차량번호는 20자 이내로 입력해 주세요.")
    private String plateNo;

    @NotNull(message = "차종을 선택해 주세요.")
    @Positive(message = "차종 정보가 올바르지 않습니다.")
    private Long vehicleTypeId;
}