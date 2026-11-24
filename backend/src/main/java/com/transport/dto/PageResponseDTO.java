package com.transport.dto;

import java.util.List;
import java.util.stream.IntStream;

import lombok.Builder;
import lombok.Data;

// 목록 응답 (수업 코드와 같은 필드 이름, 페이지 번호는 10개씩)
// 서비스: PageResponseDTO.<XxxDTO>withAll().dtoList(dtoList).pageRequestDTO(pageRequestDTO).totalCount(result.getTotalElements()).build()
@Data
public class PageResponseDTO<E> {

	private List<E> dtoList;
	private List<Integer> pageNumList;
	private PageRequestDTO pageRequestDTO;
	private boolean prev, next;
	private long totalCount;
	private int prevPage, nextPage, totalPage, current;

	@Builder(builderMethodName = "withAll")
	public PageResponseDTO(List<E> dtoList, PageRequestDTO pageRequestDTO, long totalCount) {
		this.dtoList = dtoList;
		this.pageRequestDTO = pageRequestDTO;
		this.totalCount = totalCount;

		int end = (int) (Math.ceil(pageRequestDTO.getPage() / 10.0)) * 10;
		int start = end - 9;
		int last = (int) (Math.ceil(totalCount / (double) pageRequestDTO.getSize()));
		end = Math.min(end, last);

		this.prev = start > 1;
		this.next = totalCount > end * pageRequestDTO.getSize();
		this.pageNumList = IntStream.rangeClosed(start, end).boxed().toList();
		this.prevPage = prev ? start - 1 : 0;
		this.nextPage = next ? end + 1 : 0;
		this.totalPage = last;  // 전체 페이지 수 (수업 코드는 현재 블록의 페이지 수였다)
		this.current = pageRequestDTO.getPage();
	}
}
