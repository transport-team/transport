package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.domain.Inquiry;
import com.transport.domain.Member;
import com.transport.domain.enums.InquiryStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.NotificationType;
import com.transport.dto.InquiryDTO;
import com.transport.dto.InquiryRequestDTO;
import com.transport.dto.InquirySearchDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.InquiryRepository;
import com.transport.repository.MemberRepository;

// 1:1 문의 규칙: 등록은 답변 대기로, 목록은 최신순, 알림은 첫 답변 때만 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class InquiryServiceImplTest {

	@Mock
	private InquiryRepository inquiryRepository;

	@Mock
	private MemberRepository memberRepository;

	@Mock
	private NotificationService notificationService;

	@Captor
	private ArgumentCaptor<Inquiry> inquiryCaptor;

	@Captor
	private ArgumentCaptor<Pageable> pageableCaptor;

	@InjectMocks
	private InquiryServiceImpl inquiryService;

	private final Member customer = Member.builder().id(2L).email("customer@test.com").name("김고객").role(MemberRole.CUSTOMER).build();

	@Test
	void register() {
		when(memberRepository.findById(2L)).thenReturn(Optional.of(customer));
		when(inquiryRepository.save(any(Inquiry.class))).thenAnswer(invocation -> {
			Inquiry inquiry = invocation.getArgument(0);
			ReflectionTestUtils.setField(inquiry, "id", 7L);
			return inquiry;
		});

		Long inquiryId = inquiryService.register(2L, InquiryRequestDTO.builder().title(" 배차가 늦어요 ").content("언제 배차되나요?").build());

		assertThat(inquiryId).isEqualTo(7L);
		verify(inquiryRepository).save(inquiryCaptor.capture());
		assertThat(inquiryCaptor.getValue().getStatus()).isEqualTo(InquiryStatus.WAITING);
		assertThat(inquiryCaptor.getValue().getMember()).isSameAs(customer);
		assertThat(inquiryCaptor.getValue().getTitle()).isEqualTo("배차가 늦어요");  // 앞뒤 공백은 지운다
	}

	@Test
	void registerUnknownMember() {
		when(memberRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> inquiryService.register(99L, InquiryRequestDTO.builder().title("제목").content("내용").build()))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
		verify(inquiryRepository, never()).save(any());
	}

	@Test
	void myInquiriesLatestFirst() {
		when(inquiryRepository.findMyInquiries(eq(2L), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(inquiry(8L, InquiryStatus.ANSWERED), inquiry(7L, InquiryStatus.WAITING)), PageRequest.of(0, 10), 2));

		PageResponseDTO<InquiryDTO> result = inquiryService.getMyInquiries(2L, PageRequestDTO.builder().build());

		assertThat(result.getDtoList()).extracting(InquiryDTO::getId).containsExactly(8L, 7L);
		assertThat(result.getDtoList().get(0).getMemberName()).isEqualTo("김고객");
		assertThat(result.getTotalCount()).isEqualTo(2);
		verify(inquiryRepository).findMyInquiries(eq(2L), pageableCaptor.capture());
		assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.by("id").descending());
	}

	@Test
	void adminListByStatus() {
		when(inquiryRepository.search(eq(InquiryStatus.WAITING), any(Pageable.class)))
				.thenReturn(new PageImpl<>(List.of(inquiry(7L, InquiryStatus.WAITING)), PageRequest.of(0, 10), 1));

		PageResponseDTO<InquiryDTO> result = inquiryService.getInquiries(InquirySearchDTO.builder().status(InquiryStatus.WAITING).build());

		assertThat(result.getDtoList()).extracting(InquiryDTO::getStatus).containsExactly(InquiryStatus.WAITING);
		assertThat(result.getDtoList().get(0).getMemberRole()).isEqualTo(MemberRole.CUSTOMER);
	}

	@Test
	void firstAnswerNotifies() {
		Inquiry inquiry = inquiry(7L, InquiryStatus.WAITING);
		when(inquiryRepository.findById(7L)).thenReturn(Optional.of(inquiry));

		InquiryDTO result = inquiryService.answer(7L, "오늘 오후에 배차돼요.");

		assertThat(result.getStatus()).isEqualTo(InquiryStatus.ANSWERED);
		assertThat(result.getAnswer()).isEqualTo("오늘 오후에 배차돼요.");
		assertThat(result.getAnsweredAt()).isNotNull();
		verify(notificationService).send(customer, NotificationType.INQUIRY_ANSWERED, "1:1 문의에 답변이 등록됐어요: 문의 7", null);
	}

	@Test
	void editAnswerDoesNotNotifyAgain() {
		Inquiry inquiry = inquiry(7L, InquiryStatus.WAITING);
		inquiry.answer("처음 답변", LocalDateTime.now().minusHours(1));
		when(inquiryRepository.findById(7L)).thenReturn(Optional.of(inquiry));

		InquiryDTO result = inquiryService.answer(7L, "고친 답변");

		assertThat(result.getAnswer()).isEqualTo("고친 답변");
		verify(notificationService, never()).send(any(), any(), any(), any());  // 답변을 고칠 때는 알림 없음
	}

	@Test
	void remove() {
		Inquiry inquiry = inquiry(7L, InquiryStatus.WAITING);
		when(inquiryRepository.findById(7L)).thenReturn(Optional.of(inquiry));

		inquiryService.remove(7L);

		verify(inquiryRepository).delete(inquiry);
	}

	@Test
	void unknownInquiry() {
		when(inquiryRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> inquiryService.answer(99L, "답변"))
				.isInstanceOf(CustomException.class)
				.hasMessage("문의를 찾을 수 없습니다.")
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
		verify(notificationService, never()).send(any(), any(), any(), any());
	}

	// 관리자 대시보드: 답변 대기만 센다
	@Test
	void countWaiting() {
		when(inquiryRepository.countByStatus(InquiryStatus.WAITING)).thenReturn(3L);

		assertThat(inquiryService.countWaiting()).isEqualTo(3);
	}

	private Inquiry inquiry(Long id, InquiryStatus status) {
		return Inquiry.builder().id(id).member(customer).title("문의 " + id).content("내용 " + id).status(status).build();
	}
}
