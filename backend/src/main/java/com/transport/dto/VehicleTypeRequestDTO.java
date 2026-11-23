package com.transport.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleTypeRequestDTO {

    @NotBlank(message = "차종 코드는 필수입니다.")
    @Size(max = 30, message = "차종 코드는 30자 이하여야 합니다.")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "차종 코드는 영문 대문자, 숫자, 밑줄만 사용할 수 있습니다.")
    private String code;

    @NotBlank(message = "차종 이름은 필수입니다.")
    @Size(max = 60, message = "차종 이름은 60자 이하여야 합니다.")
    private String name;

    @Min(value = 1, message = "최대 적재량은 1kg 이상이어야 합니다.")
    private int maxLoadKg;

    @Min(value = 0, message = "기본요금은 0원 이상이어야 합니다.")
    private int baseFare;

    @Min(value = 0, message = "기본 거리는 0km 이상이어야 합니다.")
    private int baseDistanceKm;

    @Min(value = 0, message = "거리요금은 0원 이상이어야 합니다.")
    private int perKmFare;

    @Min(0) @Max(500)
    private int nightRate;

    @Min(0) @Max(500)
    private int weekendRate;

    @Min(0) @Max(500)
    private int heavyRate;

    @NotNull(message = "사용 여부를 선택해 주세요.")
    private Boolean active;
}
