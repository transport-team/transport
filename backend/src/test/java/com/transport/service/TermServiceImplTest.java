package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
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

import com.transport.domain.Term;
import com.transport.dto.TermAdminDTO;
import com.transport.dto.TermDTO;
import com.transport.dto.TermRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.TermAgreementRepository;
import com.transport.repository.TermAgreementRepository.TermAgreementCount;
import com.transport.repository.TermRepository;

// 약관 관리 규칙: 개정은 새 버전 + 이전 버전 사용 중지, 같은 이름은 하나만 사용 중, 동의가 있으면 삭제 불가
// (DB 없이 Repository는 가짜로 확인)
@ExtendWith(MockitoExtension.class)
class TermServiceImplTest {

	@Mock
	private TermRepository termRepository;

	@Mock
	private TermAgreementRepository termAgreementRepository;

	@Captor
	private ArgumentCaptor<Term> termCaptor;

	@InjectMocks
	private TermServiceImpl termService;

	@Test
	void publicTermsInSignupOrder() {
		when(termRepository.findAllByActiveTrueOrderByRequiredDescIdAsc())
				.thenReturn(List.of(term(2L, "개인정보 수집 · 이용 동의", true, true), term(3L, "마케팅 정보 수신 동의", false, true)));

		List<TermDTO> result = termService.getActiveTerms();

		assertThat(result).extracting(TermDTO::getId).containsExactly(2L, 3L);
	}

	@Test
	void adminListWithAgreementCounts() {
		when(termRepository.findAllByOrderByActiveDescRequiredDescIdAsc())
				.thenReturn(List.of(term(4L, "서비스 이용약관", true, true), term(1L, "서비스 이용약관", true, false)));
		when(termAgreementRepository.countGroupByTerm()).thenReturn(List.of(count(1L, 3L)));

		List<TermAdminDTO> result = termService.getAllTerms();

		assertThat(result)
				.extracting(TermAdminDTO::getId, TermAdminDTO::isActive, TermAdminDTO::getAgreementCount)
				.containsExactly(tuple(4L, true, 0L), tuple(1L, false, 3L));  // 아직 동의가 없는 새 버전은 0명
	}

	@Test
	void register() {
		when(termRepository.existsByTitleAndActiveTrue("위치정보 이용약관")).thenReturn(false);
		when(termRepository.save(any(Term.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 5L));

		Long termId = termService.register(request(" 위치정보 이용약관 ", "v1.0"));

		assertThat(termId).isEqualTo(5L);
		verify(termRepository).save(termCaptor.capture());
		assertThat(termCaptor.getValue().getTitle()).isEqualTo("위치정보 이용약관");  // 앞뒤 공백은 지운다
		assertThat(termCaptor.getValue().isActive()).isTrue();
	}

	@Test
	void registerSameTitleInUse() {
		when(termRepository.existsByTitleAndActiveTrue("서비스 이용약관")).thenReturn(true);

		assertThatThrownBy(() -> termService.register(request("서비스 이용약관", "v2.0")))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining("같은 이름의 약관이 이미 사용 중")
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		verify(termRepository, never()).save(any());
	}

	@Test
	void reviseKeepsOldVersion() {
		Term current = term(1L, "서비스 이용약관", true, true);
		when(termRepository.findById(1L)).thenReturn(Optional.of(current));
		when(termRepository.existsByTitleAndActiveTrueAndIdNot("서비스 이용약관", 1L)).thenReturn(false);
		when(termRepository.save(any(Term.class))).thenAnswer(invocation -> withId(invocation.getArgument(0), 4L));

		Long termId = termService.revise(1L, request("서비스 이용약관", "v1.1"));

		assertThat(termId).isEqualTo(4L);
		assertThat(current.isActive()).isFalse();             // 이전 버전은 사용 중지
		assertThat(current.getContent()).isEqualTo("내용 1");  // 이전 버전 내용은 그대로 (동의 이력의 근거)
		verify(termRepository).save(termCaptor.capture());
		assertThat(termCaptor.getValue().getVersion()).isEqualTo("v1.1");
		assertThat(termCaptor.getValue().isActive()).isTrue();
	}

	@Test
	void reviseNeedsNewVersion() {
		when(termRepository.findById(1L)).thenReturn(Optional.of(term(1L, "서비스 이용약관", true, true)));

		assertThatThrownBy(() -> termService.revise(1L, request("서비스 이용약관", "v1.0")))
				.isInstanceOf(CustomException.class)
				.hasMessage("새 버전은 지금 버전(v1.0)과 달라야 해요.");
		verify(termRepository, never()).save(any());
	}

	@Test
	void reviseOnlyActiveTerm() {
		when(termRepository.findById(1L)).thenReturn(Optional.of(term(1L, "서비스 이용약관", true, false)));

		assertThatThrownBy(() -> termService.revise(1L, request("서비스 이용약관", "v1.1")))
				.isInstanceOf(CustomException.class)
				.hasMessage("사용 중인 약관만 개정할 수 있어요.");
	}

	@Test
	void reactivateBlockedWhenSameTitleInUse() {
		Term oldVersion = term(1L, "서비스 이용약관", true, false);
		when(termRepository.findById(1L)).thenReturn(Optional.of(oldVersion));
		when(termRepository.existsByTitleAndActiveTrueAndIdNot("서비스 이용약관", 1L)).thenReturn(true);  // 새 버전이 사용 중

		assertThatThrownBy(() -> termService.changeActive(1L, true))
				.isInstanceOf(CustomException.class)
				.hasMessageContaining("그 약관을 먼저 사용 중지해 주세요.");
		assertThat(oldVersion.isActive()).isFalse();
	}

	@Test
	void deactivate() {
		Term term = term(3L, "마케팅 정보 수신 동의", false, true);
		when(termRepository.findById(3L)).thenReturn(Optional.of(term));
		when(termAgreementRepository.countByTermId(3L)).thenReturn(2L);

		TermAdminDTO result = termService.changeActive(3L, false);

		assertThat(result.isActive()).isFalse();
		assertThat(result.getAgreementCount()).isEqualTo(2L);
	}

	@Test
	void removeBlockedWhenAgreed() {
		when(termRepository.findById(1L)).thenReturn(Optional.of(term(1L, "서비스 이용약관", true, true)));
		when(termAgreementRepository.countByTermId(1L)).thenReturn(5L);

		assertThatThrownBy(() -> termService.remove(1L))
				.isInstanceOf(CustomException.class)
				.hasMessage("동의한 회원이 있는 약관은 삭제할 수 없어요. 사용 중지해 주세요.");
		verify(termRepository, never()).delete(any());
	}

	@Test
	void removeWithoutAgreements() {
		Term term = term(6L, "잘못 만든 약관", false, true);
		when(termRepository.findById(6L)).thenReturn(Optional.of(term));
		when(termAgreementRepository.countByTermId(6L)).thenReturn(0L);

		termService.remove(6L);

		verify(termRepository).delete(term);
	}

	@Test
	void unknownTerm() {
		when(termRepository.findById(99L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> termService.changeActive(99L, false))
				.isInstanceOf(CustomException.class)
				.hasMessage("약관을 찾을 수 없습니다.")
				.extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
	}

	private Term term(Long id, String title, boolean required, boolean active) {
		return Term.builder().id(id).title(title).content("내용 " + id).required(required).version("v1.0").active(active).build();
	}

	private TermRequestDTO request(String title, String version) {
		return TermRequestDTO.builder().title(title).content("새 내용").required(true).version(version).build();
	}

	private Term withId(Term term, Long id) {
		ReflectionTestUtils.setField(term, "id", id);
		return term;
	}

	private TermAgreementCount count(Long termId, Long agreementCount) {
		return new TermAgreementCount() {
			@Override
			public Long getTermId() {
				return termId;
			}

			@Override
			public Long getAgreementCount() {
				return agreementCount;
			}
		};
	}
}
