package com.transport.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 공지사항 (목록 · 상세 공통)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NoticeDTO {

	private Long id;
	private String title;
	private String content;
	private String writerName;        // 작성 관리자 이름
	private int viewCount;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
