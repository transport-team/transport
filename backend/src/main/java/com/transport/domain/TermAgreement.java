package com.transport.domain;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

// 약관 동의 이력 (회원 + 약관 1건)
@Entity
@Table(name = "term_agreement", uniqueConstraints = @UniqueConstraint(
		name = "uk_term_agreement_member_term", columnNames = {"member_id", "term_id"}))
@Getter
@ToString(exclude = {"member", "term"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TermAgreement extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "term_id", nullable = false)
	private Term term;

	@Column(nullable = false)
	private LocalDateTime agreedAt;
}
