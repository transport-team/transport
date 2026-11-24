package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.enums.InquiryStatus;
import com.transport.domain.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 1:1 문의 (내 문의 · 관리자 목록 같은 모양)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InquiryDTO {

	private Long id;
	private String title;
	private String content;
	private String answer;              // 답변 전에는 null
	private LocalDateTime answeredAt;   // 답변을 고치면 그때로 바뀐다
	private InquiryStatus status;       // WAITING · ANSWERED
	private LocalDateTime createdAt;

	// 작성자 (관리자 목록에서 누가 물었는지)
	private Long memberId;
	private String memberName;
	private String memberEmail;
	private MemberRole memberRole;      // CUSTOMER · DRIVER
}
