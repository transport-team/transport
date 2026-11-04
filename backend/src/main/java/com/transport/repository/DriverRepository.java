package com.transport.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Driver;

// 기사 (팀원 2 도메인 — 팀원 2 부재 기간에 팀원 1이 최소 구현, 복귀 후 인계)
public interface DriverRepository extends JpaRepository<Driver, Long> {

	Optional<Driver> findByMemberId(Long memberId);
}
