package com.transport.domain;

import java.time.LocalDateTime;

import com.transport.domain.enums.DriverStatus;

import jakarta.persistence.*;
import lombok.*;

// 기사 (DRIVER 회원의 추가 정보, member와 1:1)
@Entity
@Table(name = "driver")
@Getter
@ToString(exclude = "member")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Driver extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false, unique = true)
	private Member member;

	@Column(nullable = false, length = 20)
	private String licenseType;  // 면허 종류

	private int careerYears;  // 경력(년)

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private DriverStatus status;

	private LocalDateTime approvedAt;

	// 관리자 승인(ACTIVE) · 중지(INACTIVE). 처음 승인할 때만 승인 시각을 남긴다 (중지 후 다시 승인해도 유지)
	public void changeStatus(DriverStatus status) {
		this.status = status;
		if (status == DriverStatus.ACTIVE && approvedAt == null) {
			this.approvedAt = LocalDateTime.now();
		}
	}
}
