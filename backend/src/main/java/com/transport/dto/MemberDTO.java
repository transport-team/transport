package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// 회원 정보 (내 정보 · 관리자 회원 목록)
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
	private boolean enabled;          // false = 관리자가 비활성화한 계정
	private LocalDateTime createdAt;  // 가입 시각
}
