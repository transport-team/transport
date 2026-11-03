package com.transport.dto;

import com.transport.domain.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 회원 정보 (내 정보)
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberDTO {

	private Long id;
	private String email;
	private String name;
	private String phone;
	private MemberRole role;
}
