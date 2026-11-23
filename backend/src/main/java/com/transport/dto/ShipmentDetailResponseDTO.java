package com.transport.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.transport.domain.Dispatch;
import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ShipmentDetailResponseDTO {

	private ReviewResponseDTO review;
    private Long id;
    private ShipmentStatus status;
    private String originAddress;
    private String originDetail;
    private double originLat;
    private double originLng;
    private String destAddress;
    private String destDetail;
    private double destLat;
    private double destLng;
    private double distanceKm;
    private Integer durationMin;
    private String cargoName;
    private int cargoWeightKg;
    private String cargoTypeName;
    private String vehicleTypeName;
    private LocalDateTime pickupAt;
    private String senderName;
    private String senderPhone;
    private String receiverName;
    private String receiverPhone;
    private String memo;
    private int fare;
    private Long assignedDriverMemberId;
    private DriverInfoDTO driverInfo;
    private PaymentInfoDTO paymentInfo;
    private List<ShipmentStatusHistoryResponseDTO> history;

    @Getter
    @Builder
    public static class DriverInfoDTO {
        private Long driverId;
        private Long memberId;
        private String driverName;
        private String phone;
        private String plateNo;
        private String vehicleTypeName;

        public static DriverInfoDTO from(Dispatch dispatch) {
            if (dispatch == null || dispatch.getDriver() == null) {
                return null;
            }
            var driver = dispatch.getDriver();
            var member = driver.getMember();
            var vehicle = dispatch.getVehicle();
            return DriverInfoDTO.builder()
                    .driverId(driver.getId())
                    .memberId(member != null ? member.getId() : null)
                    .driverName(member != null ? member.getName() : null)
                    .phone(member != null ? member.getPhone() : null)
                    .plateNo(vehicle != null ? vehicle.getPlateNo() : null)
                    .vehicleTypeName(vehicle != null && vehicle.getVehicleType() != null
                            ? vehicle.getVehicleType().getName()
                            : null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class PaymentInfoDTO {
        private Long paymentId;
        private String orderId;
        private int amount;
        private String method;
        private PaymentStatus status;
        private LocalDateTime paidAt;
        private LocalDateTime canceledAt;
        private String cancelReason;

        public static PaymentInfoDTO from(Payment payment) {
            if (payment == null) {
                return null;
            }
            return PaymentInfoDTO.builder()
                    .paymentId(payment.getId())
                    .orderId(payment.getOrderId())
                    .amount(payment.getAmount())
                    .method(payment.getMethod())
                    .status(payment.getStatus())
                    .paidAt(payment.getPaidAt())
                    .canceledAt(payment.getCanceledAt())
                    .cancelReason(payment.getCancelReason())
                    .build();
        }
    }

    public static ShipmentDetailResponseDTO from(Shipment shipment,
            Long assignedDriverMemberId, List<ShipmentStatusHistoryResponseDTO> history, ReviewResponseDTO review) {
        return from(shipment, assignedDriverMemberId, null, null, history, review);
    }

    public static ShipmentDetailResponseDTO from(Shipment shipment,
            Long assignedDriverMemberId, DriverInfoDTO driverInfo, PaymentInfoDTO paymentInfo,
            List<ShipmentStatusHistoryResponseDTO> history, ReviewResponseDTO review) {
        return ShipmentDetailResponseDTO.builder()
                .id(shipment.getId())
                .status(shipment.getStatus())
                .originAddress(shipment.getOriginAddress())
                .originDetail(shipment.getOriginDetail())
                .originLat(shipment.getOriginLat())
                .originLng(shipment.getOriginLng())
                .destAddress(shipment.getDestAddress())
                .destDetail(shipment.getDestDetail())
                .destLat(shipment.getDestLat())
                .destLng(shipment.getDestLng())
                .distanceKm(shipment.getDistanceKm())
                .durationMin(shipment.getDurationMin())
                .cargoName(shipment.getCargoName())
                .cargoWeightKg(shipment.getCargoWeightKg())
                .cargoTypeName(shipment.getCargoType().getName())
                .vehicleTypeName(shipment.getVehicleType().getName())
                .pickupAt(shipment.getPickupAt())
                .senderName(shipment.getSenderName())
                .senderPhone(shipment.getSenderPhone())
                .receiverName(shipment.getReceiverName())
                .receiverPhone(shipment.getReceiverPhone())
                .memo(shipment.getMemo())
                .fare(shipment.getFare())
                .assignedDriverMemberId(assignedDriverMemberId)
                .driverInfo(driverInfo)
                .paymentInfo(paymentInfo)
                .history(history)
                .review(review)
                .build();
    }
}