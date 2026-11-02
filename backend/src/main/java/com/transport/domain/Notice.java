package com.transport.domain;

import jakarta.persistence.*;
import lombok.*;

// 공지사항
@Entity
@Table(name = "notice")
@Getter
@ToString(exclude = "writer")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Notice extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "writer_id", nullable = false)
	private Member writer;  // 작성 관리자

	private int viewCount;
}
