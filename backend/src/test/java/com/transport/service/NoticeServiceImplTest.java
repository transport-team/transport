package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.domain.Member;
import com.transport.domain.Notice;
import com.transport.domain.enums.MemberRole;
import com.transport.dto.NoticeDTO;
import com.transport.dto.NoticeRequestDTO;
import com.transport.dto.NoticeSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.MemberRepository;
import com.transport.repository.NoticeRepository;

// 공지사항 규칙 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class NoticeServiceImplTest {

	@Mock
	private NoticeRepository noticeRepository;

	@Mock
	private MemberRepository memberRepository;

	@Captor
	private ArgumentCaptor<Notice> noticeCaptor;

	@InjectMocks
	private NoticeServiceImpl noticeService;

	private final Member admin = Member.builder().id(1L).email("admin@test.com").name("관리자").role(MemberRole.ADMIN).build();

	@Test
	void getNotices() {
		when(noticeRepository.search(eq("오픈"), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(notice(3)), PageRequest.of(0, 10), 1));

		PageResponseDTO<NoticeDTO> result = noticeService.getNotices(NoticeSearchDTO.builder().keyword("오픈").build());

		assertThat(result.getTotalCount()).isEqualTo(1);
		assertThat(result.getDtoList().get(0).getTitle()).isEqualTo("서비스 오픈 안내");
		assertThat(result.getDtoList().get(0).getWriterName()).isEqualTo("관리자");
	}

	@Test
	void getNoticeIncreasesViewCount() {
		Notice notice = notice(3);
		when(noticeRepository.findById(5L)).thenReturn(Optional.of(notice));

		NoticeDTO result = noticeService.getNotice(5L);

		assertThat(result.getViewCount()).isEqualTo(4);
		assertThat(notice.getViewCount()).isEqualTo(4);  // 트랜잭션이 끝날 때 변경 감지로 저장된다
	}

	@Test
	void unknownNotice() {
		when(noticeRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> noticeService.getNotice(99L))
				.isInstanceOf(CustomException.class)
				.hasMessage("공지를 찾을 수 없습니다.")
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	void register() {
		when(memberRepository.getReferenceById(1L)).thenReturn(admin);
		when(noticeRepository.save(any(Notice.class))).thenAnswer(invocation -> {
			Notice notice = invocation.getArgument(0);
			ReflectionTestUtils.setField(notice, "id", 5L);
			return notice;
		});

		Long noticeId = noticeService.register(1L, request("추석 연휴 배송 안내", "배차가 늦어질 수 있어요."));

		assertThat(noticeId).isEqualTo(5L);
		verify(noticeRepository).save(noticeCaptor.capture());
		assertThat(noticeCaptor.getValue().getWriter()).isSameAs(admin);  // 작성자 = 로그인한 관리자
		assertThat(noticeCaptor.getValue().getViewCount()).isZero();
	}

	@Test
	void modify() {
		Notice notice = notice(3);
		when(noticeRepository.findById(5L)).thenReturn(Optional.of(notice));

		NoticeDTO result = noticeService.modify(5L, request("바뀐 제목", "바뀐 내용"));

		assertThat(result.getTitle()).isEqualTo("바뀐 제목");
		assertThat(notice.getContent()).isEqualTo("바뀐 내용");
		assertThat(notice.getViewCount()).isEqualTo(3);  // 수정은 조회수를 올리지 않는다
	}

	@Test
	void remove() {
		Notice notice = notice(3);
		when(noticeRepository.findById(5L)).thenReturn(Optional.of(notice));

		noticeService.remove(5L);

		verify(noticeRepository).delete(notice);
	}

	private Notice notice(int viewCount) {
		return Notice.builder().id(5L).title("서비스 오픈 안내").content("운송서비스가 열렸습니다.").writer(admin).viewCount(viewCount).build();
	}

	private NoticeRequestDTO request(String title, String content) {
		return NoticeRequestDTO.builder().title(title).content(content).build();
	}
}
