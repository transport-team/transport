package com.transport.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

// 기사: 본인 확정 배차 조회 응답
@Getter
@Builder
public class DriverAssignedDispatchResponseDTO {

    private Long dispatchId;
    private Long shipmentId;
    private Long vehicleId;
    private Long vehicleTypeId;

    private String cargoName;
    private int cargoWeightKg;
    private int fare;

    private String originAddress;
    private String originDetail;
    private String destAddress;
    private String destDetail;

    private LocalDateTime pickupAt;
    private Integer durationMin;

    private String receiverName;
    private String receiverPhone;
    private String memo;

    private String status;
    private LocalDateTime assignedAt;
}
