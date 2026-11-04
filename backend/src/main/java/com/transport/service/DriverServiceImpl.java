package com.transport.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// DriverService 구현 (팀원 2 도메인 — 팀원 1이 최소 구현)
@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

	private final DriverRepository driverRepository;

	@Override
	public Long register(Member member, String licenseType, int careerYears) {
		Driver driver = driverRepository.save(Driver.builder()
				.member(member)
				.licenseType(licenseType)
				.careerYears(careerYears)
				.status(DriverStatus.PENDING)  // 관리자가 승인하면 ACTIVE
				.build());

		log.info("기사 등록(승인 대기): driverId={}, memberId={}", driver.getId(), member.getId());
		return driver.getId();
	}

	@Override
	@Transactional(readOnly = true)
	public DriverStatus getStatus(Long memberId) {
		return driverRepository.findByMemberId(memberId)
				.map(Driver::getStatus)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보가 없습니다."));
	}
}
