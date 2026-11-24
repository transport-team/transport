package com.transport.dto;

import com.transport.domain.Payment;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PaymentConfirmResponseDTO {

	private Long shipmentId;
	private String orderId;
	private String paymentKey;
	private int amount;
	private PaymentStatus paymentStatus;
	private ShipmentStatus shipmentStatus;

	public static PaymentConfirmResponseDTO from(Payment payment) {
		return PaymentConfirmResponseDTO.builder()
				.shipmentId(payment.getShipment().getId())
				.orderId(payment.getOrderId())
				.paymentKey(payment.getPaymentKey())
				.amount(payment.getAmount())
				.paymentStatus(payment.getStatus())
				.shipmentStatus(payment.getShipment().getStatus())
				.build();
	}
}
