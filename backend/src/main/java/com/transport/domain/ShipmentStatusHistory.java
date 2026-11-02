package com.transport.domain;

import java.time.LocalDateTime;

import com.transport.domain.enums.ShipmentStatus;

import jakarta.persistence.*;
import lombok.*;

// 배송 상태 변경 이력 (모든 상태 전이마다 1건)
@Entity
@Table(name = "shipment_status_history")
@Getter
@ToString(exclude = {"shipment", "changedBy"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentStatusHistory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@Enumerated(EnumType.STRING)
	@Column(columnDefinition = "VARCHAR(20)")
	private ShipmentStatus fromStatus;  // 신규 신청이면 NULL

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private ShipmentStatus toStatus;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "changed_by")
	private Member changedBy;  // 변경자 (시스템이면 NULL)

	private String memo;  // 사유

	@Column(nullable = false)
	private LocalDateTime changedAt;
}
