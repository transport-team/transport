package com.transport.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

// 페이지 번호 계산 (10개씩 보기)
class PageResponseDTOTest {

	@Test
	void firstBlock() {
		PageResponseDTO<String> response = withPage(1, 123);

		assertThat(response.getPageNumList()).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
		assertThat(response.isPrev()).isFalse();
		assertThat(response.isNext()).isTrue();
		assertThat(response.getNextPage()).isEqualTo(11);
		assertThat(response.getTotalPage()).isEqualTo(13);
		assertThat(response.getCurrent()).isEqualTo(1);
	}

	@Test
	void lastBlock() {
		PageResponseDTO<String> response = withPage(12, 123);

		assertThat(response.getPageNumList()).containsExactly(11, 12, 13);
		assertThat(response.isPrev()).isTrue();
		assertThat(response.getPrevPage()).isEqualTo(10);
		assertThat(response.isNext()).isFalse();
		assertThat(response.getTotalPage()).isEqualTo(13);
	}

	@Test
	void exactlyTenPages() {
		PageResponseDTO<String> response = withPage(1, 100);

		assertThat(response.getPageNumList()).hasSize(10);
		assertThat(response.isNext()).isFalse();
		assertThat(response.getTotalPage()).isEqualTo(10);
	}

	@Test
	void empty() {
		PageResponseDTO<String> response = withPage(1, 0);

		assertThat(response.getPageNumList()).isEmpty();
		assertThat(response.isPrev()).isFalse();
		assertThat(response.isNext()).isFalse();
		assertThat(response.getTotalPage()).isZero();
	}

	private PageResponseDTO<String> withPage(int page, long totalCount) {
		return PageResponseDTO.<String>withAll()
				.dtoList(List.of())
				.pageRequestDTO(PageRequestDTO.builder().page(page).size(10).build())
				.totalCount(totalCount)
				.build();
	}
}
