package com.transport.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;

// 회원
public interface MemberRepository extends JpaRepository<Member, Long> {

	boolean existsByEmail(String email);

	Optional<Member> findByEmail(String email);

	// 관리자 회원 목록: 역할 · 상태 · 이름 또는 이메일로 검색 (조건이 null이면 뺀다)
	@Query("select m from Member m"
			+ " where (:role is null or m.role = :role)"
			+ " and (:enabled is null or m.enabled = :enabled)"
			+ " and (:keyword is null or m.name like concat('%', :keyword, '%') or m.email like concat('%', :keyword, '%'))")
	Page<Member> search(@Param("role") MemberRole role, @Param("enabled") Boolean enabled, @Param("keyword") String keyword,
			Pageable pageable);
}
