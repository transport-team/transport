package com.transport.dto;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

// 목록 요청 (?page=1&size=10). 검색 조건은 상속해서 추가한다 (예: status, from, to, keyword)
// 컨트롤러: list(@Valid PageRequestDTO pageRequestDTO)
@Data
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class PageRequestDTO {

	@Builder.Default
	@Min(value = 1, message = "page는 1 이상이어야 합니다.")
	private int page = 1;

	@Builder.Default
	@Min(value = 1, message = "size는 1 이상이어야 합니다.")
	@Max(value = 100, message = "size는 100 이하여야 합니다.")
	private int size = 10;

	// 서비스: pageRequestDTO.getPageable(Sort.by("id").descending())
	public Pageable getPageable(Sort sort) {
		return PageRequest.of(page - 1, size, sort);
	}
}
