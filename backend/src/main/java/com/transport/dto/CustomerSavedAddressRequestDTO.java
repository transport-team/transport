package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerSavedAddressRequestDTO(
        @NotBlank(message = "배송지 별명을 입력해 주세요.")
        @Size(max = 50, message = "배송지 별명은 50자 이하로 입력해 주세요.")
        String label,
        @NotBlank(message = "주소를 선택해 주세요.")
        @Size(max = 500, message = "주소는 500자 이하로 입력해 주세요.")
        String address,
        @Size(max = 200, message = "상세주소는 200자 이하로 입력해 주세요.")
        String detail) {
}
