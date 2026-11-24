package com.transport.domain;

import jakarta.persistence.*;
import lombok.*;

// 리뷰 (배송당 1개)
@Entity
@Table(name = "review")
@Getter
@ToString(exclude = {"shipment", "customer", "driver"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Review extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false, unique = true)
	private Shipment shipment;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Member customer;  // 작성 고객

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id", nullable = false)
	private Driver driver;  // 평가 대상 기사

	@Column(nullable = false, columnDefinition = "TINYINT")
	private int rating;  // 1~5

	@Column(length = 500)
	private String content;
}
