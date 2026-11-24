package com.transport.domain;

import com.transport.domain.enums.ShipmentImageType;

import jakarta.persistence.*;
import lombok.*;

// 배송 사진 (상차 · 인수증)
@Entity
@Table(name = "shipment_image")
@Getter
@ToString(exclude = {"shipment", "uploadedBy"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentImage extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "shipment_id", nullable = false)
	private Shipment shipment;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private ShipmentImageType type;

	@Column(nullable = false)
	private String fileName;  // UUID_원본명

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "uploaded_by", nullable = false)
	private Member uploadedBy;  // 올린 기사 (회원)
}
