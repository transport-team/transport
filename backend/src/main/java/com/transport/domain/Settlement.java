package com.transport.domain;

import java.time.LocalDateTime;

import com.transport.domain.enums.SettlementStatus;

import jakarta.persistence.*;
import lombok.*;

// 기사 월 정산 (기사 + 정산월 1건)
@Entity
@Table(name = "settlement", uniqueConstraints = @UniqueConstraint(
		name = "uk_settlement_driver_month", columnNames = {"driver_id", "settle_month"}))
@Getter
@ToString(exclude = "driver")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Settlement extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "driver_id", nullable = false)
	private Driver driver;

	@Column(nullable = false, columnDefinition = "CHAR(7)")
	private String settleMonth;  // 예: 2026-12

	private int completedCount;  // 완료 건수

	private int totalFare;  // 운임 합계

	private int payoutAmount;  // 지급액

	private int commissionRate;  // 수수료율(%)

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private SettlementStatus status;

	private LocalDateTime paidAt;  // 지급일
}
