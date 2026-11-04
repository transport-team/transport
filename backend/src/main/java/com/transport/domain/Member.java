package com.transport.domain;

import com.transport.domain.enums.MemberRole;

import jakarta.persistence.*;
import lombok.*;

// 회원 (고객 · 기사 · 관리자 공통)
@Entity
@Table(name = "member")
@Getter
@ToString(exclude = "password")
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Member extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 100)
	private String email;  // 로그인 아이디

	@Column(nullable = false)
	private String password;  // BCrypt

	@Column(nullable = false)
	private String name;

	@Column(nullable = false)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private MemberRole role;

	private boolean enabled;

	// 내 정보 수정: 연락처
	public void changePhone(String phone) {
		this.phone = phone;
	}

	// 비밀번호 변경: 암호화(BCrypt)한 값을 받는다
	public void changePassword(String encodedPassword) {
		this.password = encodedPassword;
	}
}
