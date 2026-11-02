package com.transport.domain;

import jakarta.persistence.*;
import lombok.*;

// 약관
@Entity
@Table(name = "term")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Term extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	private boolean required;  // 필수 동의 여부

	@Column(nullable = false, length = 20)
	private String version;  // 예: v1.0

	private boolean active;
}
