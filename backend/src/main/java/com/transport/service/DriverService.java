package com.transport.service;

import java.util.Optional;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;
import com.transport.dto.DriverDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.PageResponseDTO;

// 기사 (팀원 2 도메인 — 팀원 2 부재 기간에 팀원 1이 작성, 복귀 후 인계)
// 다른 도메인은 이 메서드로만 기사 정보를 다룬다 (docs/collaboration.md 약속 표)
public interface DriverService {

	// 기사 회원가입 → 승인 대기(PENDING) 기사 생성 → 기사 id
	Long register(Member member, String licenseType, int careerYears);

	// 기사 상태 (로그인할 때 승인 여부 확인)
	DriverStatus getStatus(Long memberId);

	// 관리자 기사 목록 (상태 · 이름 · 이메일 검색, 최신 가입 순)
	PageResponseDTO<DriverDTO> getDrivers(DriverSearchDTO driverSearchDTO);

	// 관리자 승인(ACTIVE) · 중지(INACTIVE) → 바뀐 기사 정보
	DriverDTO changeStatus(Long driverId, DriverStatus status);

	Optional<Driver> findById(Long id);

	default DriverDTO entityToDTO(Driver driver) {
		Member member = driver.getMember();
		return DriverDTO.builder()
				.id(driver.getId())
				.memberId(member.getId())
				.name(member.getName())
				.email(member.getEmail())
				.phone(member.getPhone())
				.licenseType(driver.getLicenseType())
				.careerYears(driver.getCareerYears())
				.status(driver.getStatus())
				.approvedAt(driver.getApprovedAt())
				.createdAt(driver.getCreatedAt())
				.build();
	}
}
