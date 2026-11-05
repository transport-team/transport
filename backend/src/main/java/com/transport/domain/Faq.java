package com.transport.domain;

import jakarta.persistence.*;
import lombok.*;

// 자주 묻는 질문
@Entity
@Table(name = "faq")
@Getter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Faq extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 30)
	private String category;  // 배송 · 결제 · 회원

	@Column(nullable = false)
	private String question;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String answer;

	private int sortOrder;

	private boolean active;  // 노출 여부

	// FAQ 수정: 분류 · 질문 · 답변 · 순서 · 노출 (숨기기는 active = false)
	public void change(String category, String question, String answer, int sortOrder, boolean active) {
		this.category = category;
		this.question = question;
		this.answer = answer;
		this.sortOrder = sortOrder;
		this.active = active;
	}
}
