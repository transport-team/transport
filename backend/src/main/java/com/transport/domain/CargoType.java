package com.transport.domain;

import jakarta.persistence.*;
import lombok.*;

// 화물 종류
@Entity
@Table(name = "cargo_type")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CargoType extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 30)
	private String name;  // 일반 · 파손주의 · 가구/가전

	private int extraFee;  // 추가요금(원)

	private String handlingNote;  // 기사에게 보여줄 취급 안내

	private boolean active;
}
