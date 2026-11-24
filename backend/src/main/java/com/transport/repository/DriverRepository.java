package com.transport.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.Driver;
import com.transport.domain.enums.DriverStatus;

// 기사 (팀원 2 도메인 — 팀원 2 부재 기간에 팀원 1이 작성, 복귀 후 인계)
public interface DriverRepository extends JpaRepository<Driver, Long> {

	Optional<Driver> findByMemberId(Long memberId);

	// 관리자 기사 목록: 상태 · 이름 또는 이메일로 검색 (조건이 null이면 빼고), 회원 정보까지 한 번에 가져온다
	@Query(value = "select d from Driver d join fetch d.member m"
			+ " where (:status is null or d.status = :status)"
			+ " and (:keyword is null or m.name like concat('%', :keyword, '%') or m.email like concat('%', :keyword, '%'))",
			countQuery = "select count(d) from Driver d join d.member m"
			+ " where (:status is null or d.status = :status)"
			+ " and (:keyword is null or m.name like concat('%', :keyword, '%') or m.email like concat('%', :keyword, '%'))")
	Page<Driver> search(@Param("status") DriverStatus status, @Param("keyword") String keyword, Pageable pageable);
}
