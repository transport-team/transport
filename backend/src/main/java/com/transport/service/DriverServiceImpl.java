package com.transport.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.enums.DriverStatus;
import com.transport.dto.DriverDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// DriverService 구현 (팀원 2 도메인 — 팀원 1이 작성)
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

	@Override
	@Transactional(readOnly = true)
	public PageResponseDTO<DriverDTO> getDrivers(DriverSearchDTO driverSearchDTO) {
		Page<Driver> result = driverRepository.search(driverSearchDTO.getStatus(), driverSearchDTO.getKeyword(),
				driverSearchDTO.getPageable(Sort.by("id").descending()));
		List<DriverDTO> dtoList = result.getContent().stream()
				.map(this::entityToDTO)
				.toList();

		return PageResponseDTO.<DriverDTO>withAll()
				.dtoList(dtoList)
				.pageRequestDTO(driverSearchDTO)
				.totalCount(result.getTotalElements())
				.build();
	}

	@Override
	public DriverDTO changeStatus(Long driverId, DriverStatus status) {
		if (status == DriverStatus.PENDING) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "승인 대기로 되돌릴 수 없습니다.");
		}
		Driver driver = driverRepository.findById(driverId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보가 없습니다."));
		driver.changeStatus(status); // 변경 감지로 저장 (트랜잭션이 끝날 때 UPDATE)

		log.info("기사 상태 변경: driverId={}, status={}", driverId, status);
		return entityToDTO(driver);
	}

	// 기사 ID로 기사 정보 조회
	@Override
	public Optional<Driver> findById(Long id) {
		return driverRepository.findById(id);
	}
}
