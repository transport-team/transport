package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.domain.Faq;
import com.transport.dto.FaqDTO;
import com.transport.dto.FaqRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.FaqRepository;

// FAQ 규칙 (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class FaqServiceImplTest {

	@Mock
	private FaqRepository faqRepository;

	@Captor
	private ArgumentCaptor<Faq> faqCaptor;

	@InjectMocks
	private FaqServiceImpl faqService;

	@Test
	void publicFaqsWithoutCategory() {
		when(faqRepository.findByActiveTrueOrderBySortOrderAscIdAsc()).thenReturn(List.of(faq(1L, "결제", true), faq(2L, "배송", true)));

		List<FaqDTO> result = faqService.getFaqs(null);

		assertThat(result).extracting(FaqDTO::getId).containsExactly(1L, 2L);
		verify(faqRepository, never()).findAllByOrderBySortOrderAscIdAsc();  // 공개 목록은 숨긴 FAQ를 가져오지 않는다
	}

	@Test
	void publicFaqsByCategory() {
		when(faqRepository.findByActiveTrueAndCategoryOrderBySortOrderAscIdAsc("결제")).thenReturn(List.of(faq(1L, "결제", true)));

		List<FaqDTO> result = faqService.getFaqs("결제");

		assertThat(result).extracting(FaqDTO::getCategory).containsExactly("결제");
	}

	@Test
	void adminSeesHiddenFaqs() {
		when(faqRepository.findAllByOrderBySortOrderAscIdAsc()).thenReturn(List.of(faq(1L, "결제", true), faq(3L, "회원", false)));

		List<FaqDTO> result = faqService.getAllFaqs();

		assertThat(result).extracting(FaqDTO::isActive).containsExactly(true, false);
	}

	@Test
	void register() {
		when(faqRepository.save(any(Faq.class))).thenAnswer(invocation -> {
			Faq faq = invocation.getArgument(0);
			ReflectionTestUtils.setField(faq, "id", 7L);
			return faq;
		});

		Long faqId = faqService.register(request("회원", false));

		assertThat(faqId).isEqualTo(7L);
		verify(faqRepository).save(faqCaptor.capture());
		assertThat(faqCaptor.getValue().getCategory()).isEqualTo("회원");
		assertThat(faqCaptor.getValue().isActive()).isFalse();
	}

	@Test
	void modifyAndHide() {
		Faq faq = faq(1L, "결제", true);
		when(faqRepository.findById(1L)).thenReturn(Optional.of(faq));

		FaqDTO result = faqService.modify(1L, request("배송", false));

		assertThat(result.getCategory()).isEqualTo("배송");
		assertThat(faq.isActive()).isFalse();  // 숨기기 = 노출 끄기
		assertThat(faq.getSortOrder()).isEqualTo(5);
	}

	@Test
	void unknownFaq() {
		when(faqRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> faqService.remove(99L))
				.isInstanceOf(CustomException.class)
				.hasMessage("FAQ를 찾을 수 없습니다.")
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	void remove() {
		Faq faq = faq(1L, "결제", true);
		when(faqRepository.findById(1L)).thenReturn(Optional.of(faq));

		faqService.remove(1L);

		verify(faqRepository).delete(faq);
	}

	private Faq faq(Long id, String category, boolean active) {
		return Faq.builder().id(id).category(category).question("질문 " + id).answer("답변 " + id).sortOrder(id.intValue()).active(active).build();
	}

	private FaqRequestDTO request(String category, boolean active) {
		return FaqRequestDTO.builder().category(category).question("바뀐 질문").answer("바뀐 답변").sortOrder(5).active(active).build();
	}
}
