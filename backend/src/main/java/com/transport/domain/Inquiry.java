package com.transport.domain;

import java.time.LocalDateTime;

import com.transport.domain.enums.InquiryStatus;

import jakarta.persistence.*;
import lombok.*;

// 1:1 문의
@Entity
@Table(name = "inquiry")
@Getter
@ToString(exclude = "member")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Inquiry extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;  // 작성 회원

	@Column(nullable = false)
	private String title;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(columnDefinition = "TEXT")
	private String answer;

	private LocalDateTime answeredAt;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private InquiryStatus status;

	// 관리자 답변 등록 · 수정: 답변 완료로 바꾸고 답변일을 남긴다 (답변을 고치면 답변일도 그때로)
	public void answer(String answer, LocalDateTime answeredAt) {
		this.answer = answer;
		this.answeredAt = answeredAt;
		this.status = InquiryStatus.ANSWERED;
	}
}
