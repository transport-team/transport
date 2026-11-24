package com.transport.dto;

import com.transport.domain.enums.MemberRole;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

// 관리자 회원 목록 검색 (?role=CUSTOMER&enabled=false&keyword=김&page=1)
@Getter
@Setter
@ToString(callSuper = true)
@SuperBuilder
@NoArgsConstructor
public class MemberSearchDTO extends PageRequestDTO {

	private MemberRole role;   // 없으면 전체
	private Boolean enabled;   // 없으면 전체 (false = 비활성화된 회원만)
	private String keyword;    // 이름 · 이메일 일부
}
