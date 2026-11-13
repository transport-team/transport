package com.transport.dto;

import java.time.LocalDateTime;

import lombok.Builder;
import lombok.Getter;

// 기사: 지난 배차 신청 및 배송 내역 응답
@Getter
@Builder
public class DriverDispatchHistoryResponseDTO {

    private Long applicationId;
    private Long shipmentId;

    private String applicationStatus;
    private String shipmentStatus;

    private String cargoName;
    private int cargoWeightKg;
    private Long vehicleTypeId;
    private int fare;

    private String originAddress;
    private String destAddress;
    private LocalDateTime pickupAt;

    private LocalDateTime appliedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}