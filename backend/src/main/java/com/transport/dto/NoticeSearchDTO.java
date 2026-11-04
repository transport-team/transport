package com.transport.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

// 공지 목록 검색 (?keyword=배송&page=1)
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class NoticeSearchDTO extends PageRequestDTO {

	private String keyword;  // 제목 일부
}
