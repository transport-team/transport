package com.transport.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DispatchApplicantResponseDTO {

    private Long applicationId;  // 기사 지원 신청 ID

    private Long driverId;       // 기사 ID

    private String licenseType;  // 면허 종류

    private int careerYears;     // 운전 경력

    private Long vehicleId;      // 차량 ID

    private String plateNo;      // 차량번호

    private Long vehicleTypeId;  // 차종 ID

}
