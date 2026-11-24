package com.transport.service;

import com.transport.domain.Notice;
import com.transport.dto.NoticeDTO;
import com.transport.dto.NoticeRequestDTO;
import com.transport.dto.NoticeSearchDTO;
import com.transport.dto.PageResponseDTO;

// 공지사항 (팀원 1)
public interface NoticeService {

	// 공지 목록: 제목 검색, 최신순
	PageResponseDTO<NoticeDTO> getNotices(NoticeSearchDTO noticeSearchDTO);

	// 공지 상세: 볼 때마다 조회수 1 증가
	NoticeDTO getNotice(Long noticeId);

	// 관리자: 공지 작성 (작성자 = 로그인한 관리자) → 새 공지 id
	Long register(Long writerId, NoticeRequestDTO noticeRequestDTO);

	// 관리자: 공지 수정 → 바뀐 공지
	NoticeDTO modify(Long noticeId, NoticeRequestDTO noticeRequestDTO);

	// 관리자: 공지 삭제
	void remove(Long noticeId);

	default NoticeDTO entityToDTO(Notice notice) {
		return NoticeDTO.builder()
				.id(notice.getId())
				.title(notice.getTitle())
				.content(notice.getContent())
				.writerName(notice.getWriter().getName())
				.viewCount(notice.getViewCount())
				.createdAt(notice.getCreatedAt())
				.updatedAt(notice.getUpdatedAt())
				.build();
	}
}
