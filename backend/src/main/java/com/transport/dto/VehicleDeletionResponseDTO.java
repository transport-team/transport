package com.transport.dto;

import java.time.LocalDateTime;
import com.transport.domain.VehicleDeletionRequest;
import com.transport.domain.enums.VehicleRegistrationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VehicleDeletionResponseDTO {
    private Long id;
    private Long vehicleId;
    private Long driverId;
    private String driverName;
    private String plateNo;
    private String vehicleTypeName;
    private VehicleRegistrationStatus status;
    private String rejectionReason;
    private LocalDateTime createdAt;
    private LocalDateTime reviewedAt;

    public static VehicleDeletionResponseDTO from(VehicleDeletionRequest request) {
        return VehicleDeletionResponseDTO.builder()
                .id(request.getId())
                .vehicleId(request.getVehicle().getId())
                .driverId(request.getDriver().getId())
                .driverName(request.getDriver().getMember().getName())
                .plateNo(request.getVehicle().getPlateNo())
                .vehicleTypeName(request.getVehicle().getVehicleType().getName())
                .status(request.getStatus())
                .rejectionReason(request.getRejectionReason())
                .createdAt(request.getCreatedAt())
                .reviewedAt(request.getReviewedAt())
                .build();
    }
}
