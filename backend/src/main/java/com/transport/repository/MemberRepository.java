package com.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Member;

// 회원
public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);
}
