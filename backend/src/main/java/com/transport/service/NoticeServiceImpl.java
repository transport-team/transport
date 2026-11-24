package com.transport.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Notice;
import com.transport.dto.NoticeDTO;
import com.transport.dto.NoticeRequestDTO;
import com.transport.dto.NoticeSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.MemberRepository;
import com.transport.repository.NoticeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// NoticeService 구현
@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class NoticeServiceImpl implements NoticeService {

	private final NoticeRepository noticeRepository;
	private final MemberRepository memberRepository;

	@Override
	@Transactional(readOnly = true)
	public PageResponseDTO<NoticeDTO> getNotices(NoticeSearchDTO noticeSearchDTO) {
		Page<Notice> result = noticeRepository.search(noticeSearchDTO.getKeyword(),
				noticeSearchDTO.getPageable(Sort.by("id").descending()));
		List<NoticeDTO> dtoList = result.getContent().stream()
				.map(this::entityToDTO)
				.toList();

		return PageResponseDTO.<NoticeDTO>withAll()
				.dtoList(dtoList)
				.pageRequestDTO(noticeSearchDTO)
				.totalCount(result.getTotalElements())
				.build();
	}

	@Override
	public NoticeDTO getNotice(Long noticeId) {
		Notice notice = findNotice(noticeId);
		notice.increaseViewCount();  // 변경 감지로 저장 (트랜잭션이 끝날 때 UPDATE)
		return entityToDTO(notice);
	}

	@Override
	public Long register(Long writerId, NoticeRequestDTO noticeRequestDTO) {
		Notice notice = noticeRepository.save(Notice.builder()
				.title(noticeRequestDTO.getTitle())
				.content(noticeRequestDTO.getContent())
				.writer(memberRepository.getReferenceById(writerId))  // 로그인한 관리자 (조회 없이 id로 연결)
				.build());

		log.info("공지 작성: id={}, writerId={}", notice.getId(), writerId);
		return notice.getId();
	}

	@Override
	public NoticeDTO modify(Long noticeId, NoticeRequestDTO noticeRequestDTO) {
		Notice notice = findNotice(noticeId);
		notice.change(noticeRequestDTO.getTitle(), noticeRequestDTO.getContent());
		return entityToDTO(notice);
	}

	@Override
	public void remove(Long noticeId) {
		noticeRepository.delete(findNotice(noticeId));
		log.info("공지 삭제: id={}", noticeId);
	}

	private Notice findNotice(Long noticeId) {
		return noticeRepository.findById(noticeId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "공지를 찾을 수 없습니다."));
	}
}
