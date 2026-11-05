package com.transport.dto;

import com.transport.domain.enums.InquiryStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

// 관리자 문의 목록 검색 (?status=WAITING&page=1)
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class InquirySearchDTO extends PageRequestDTO {

	private InquiryStatus status;  // 없으면 전체 (WAITING = 답변 대기만)
}
