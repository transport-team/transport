package com.transport.security;

import java.util.Collection;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.transport.domain.enums.MemberRole;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

// 로그인 사용자 (액세스 토큰 안의 값이라 DB를 다시 조회하지 않는다)
// 컨트롤러에서 꺼내기: @AuthenticationPrincipal LoginMember loginMember → loginMember.getId()
@Getter
@ToString
@AllArgsConstructor
public class LoginMember {

	private final Long id;
	private final String email;
	private final MemberRole role;

	// 권한 이름은 ROLE_ + 역할 → hasRole('ADMIN')으로 검사
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
	}
}
