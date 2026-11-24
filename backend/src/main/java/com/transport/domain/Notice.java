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

	// 공지 수정: 제목 · 내용
	public void change(String title, String content) {
		this.title = title;
		this.content = content;
	}

	// 상세를 볼 때마다 조회수 1 증가
	public void increaseViewCount() {
		this.viewCount++;
	}
}
