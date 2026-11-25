package com.transport.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

// 기사 본인 면허·경력·자격증명 수정. 관리자 승인 상태는 입력받지 않는다.
@Data
public class DriverProfileUpdateRequestDTO {

    @NotBlank(message = "면허 종류를 입력해 주세요.")
    @Size(max = 20, message = "면허 종류는 20자 이하로 입력해 주세요.")
    private String licenseType;

    @NotNull(message = "경력을 입력해 주세요.")
    @Min(value = 0, message = "경력은 0년 이상이어야 합니다.")
    @Max(value = 60, message = "경력은 60년 이하이어야 합니다.")
    private Integer careerYears;

    @Size(max = 200, message = "자격증명은 200자 이하로 입력해 주세요.")
    private String certifications;
}
