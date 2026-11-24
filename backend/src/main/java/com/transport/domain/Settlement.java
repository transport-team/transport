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

	@Column(name = "paid_by_admin_email", length = 100)
	private String paidByAdminEmail; // 지급 확인을 기록한 관리자 (과거 기록은 null)

	@Column(name = "transfer_reference", length = 100)
	private String transferReference; // 실제 송금 확인번호/메모 (은행 자동 연동 X)

	// 지급 전 정산 초안만 변경할 수 있다.
	public void refreshDraft(int count, int fare, int rate, int payout) {
		if (this.status != SettlementStatus.PENDING) {
			throw new IllegalStateException("지급 완료된 정산은 수정할 수 없습니다.");
		}
		this.completedCount = count;
		this.totalFare = fare;
		this.commissionRate = rate;
		this.payoutAmount = payout;
	}

	// 실제 송금 후 관리자 수동 확인 기록 (은행 송금은 수행하지 않는다).
	public void markPaid(LocalDateTime now, String adminEmail, String reference) {
		if (this.status != SettlementStatus.PENDING) {
			throw new IllegalStateException("이미 지급 처리된 정산입니다.");
		}
		this.status = SettlementStatus.PAID;
		this.paidAt = now;
		this.paidByAdminEmail = adminEmail;
		this.transferReference = reference;
	}
}
