// AdminPaymentResponseDTO.java
package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.Payment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminPaymentResponseDTO {

	private Long id;
	private Long shipmentId;
	private ShipmentStatus shipmentStatus;
	private String customerName;
	private String customerEmail;
	private String cargoName;
	private String orderId;
	private String paymentKey;
	private int amount;
	private String method;
	private PaymentStatus status;
	private LocalDateTime paidAt;
	private LocalDateTime canceledAt;
	private String cancelReason;
	private LocalDateTime createdAt;

	public static AdminPaymentResponseDTO from(Payment payment) {
		return AdminPaymentResponseDTO.builder()
				.id(payment.getId())
				.shipmentId(payment.getShipment().getId())
				.shipmentStatus(payment.getShipment().getStatus())
				.customerName(payment.getShipment().getCustomer().getName())
				.customerEmail(payment.getShipment().getCustomer().getEmail())
				.cargoName(payment.getShipment().getCargoName())
				.orderId(payment.getOrderId())
				.paymentKey(payment.getPaymentKey())
				.amount(payment.getAmount())
				.method(payment.getMethod())
				.status(payment.getStatus())
				.paidAt(payment.getPaidAt())
				.canceledAt(payment.getCanceledAt())
				.cancelReason(payment.getCancelReason())
				.createdAt(payment.getCreatedAt())
				.build();
	}
}