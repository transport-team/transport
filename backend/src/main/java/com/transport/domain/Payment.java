package com.transport.domain;

import java.time.LocalDateTime;

import com.transport.domain.enums.PaymentStatus;

import jakarta.persistence.*;
import lombok.*;

// 결제 (배송과 1:1)
@Entity
@Table(name = "payment")
@Getter
@ToString(exclude = "shipment")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Payment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false, unique = true)
	private Shipment shipment;

	@Column(nullable = false, unique = true, length = 64)
	private String orderId;  // 주문번호 (재시도 시 새로 발급)

	@Column(length = 200)
	private String paymentKey;  // 토스 결제 키 (승인 후)

	private int amount;  // 결제 금액 (승인 전 요청 금액과 비교)

	private String method;  // 결제 수단

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private PaymentStatus status;

	private LocalDateTime paidAt;

	private LocalDateTime canceledAt;

	private String cancelReason;
}
