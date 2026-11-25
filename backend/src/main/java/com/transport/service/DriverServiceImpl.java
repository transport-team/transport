package com.transport.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.DispatchApplication;
import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverDTO;
import com.transport.dto.DriverProfileUpdateRequestDTO;
import com.transport.dto.DriverSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// DriverService 구현 (팀원 2 도메인 — 팀원 1이 작성)
@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class DriverServiceImpl implements DriverService {

	// 중지를 막는 배송 상태: 배정 ~ 도착 (status.md)
	private static final Set<ShipmentStatus> ONGOING_STATUSES = Set.of(
			ShipmentStatus.ASSIGNED, ShipmentStatus.PICKED_UP, ShipmentStatus.IN_TRANSIT, ShipmentStatus.ARRIVED);

	private final DriverRepository driverRepository;
	private final DispatchRepository dispatchRepository;
	private final DispatchApplicationRepository applicationRepository;
	private final NotificationService notificationService;  // 팀원 4 약속 메서드 (승인 · 중지 알림)

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
	@Transactional(readOnly = true)
	public DriverDTO getMyProfile(Long memberId) {
		Driver driver = driverRepository.findByMemberId(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보가 없습니다."));
		return entityToDTO(driver);
	}

	@Override
	public DriverDTO updateMyProfile(Long memberId, DriverProfileUpdateRequestDTO request) {
		Driver driver = driverRepository.findByMemberId(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보가 없습니다."));
		String certifications = request.getCertifications() == null ? null : request.getCertifications().trim();
		driver.updateProfile(request.getLicenseType().trim(), request.getCareerYears(),
				certifications == null || certifications.isEmpty() ? null : certifications);
		return entityToDTO(driver);
	}

	@Override
	public DriverDTO changeStatus(Long driverId, DriverStatus status) {
		if (status == DriverStatus.PENDING) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "승인 대기로 되돌릴 수 없습니다.");
		}
		Driver driver = driverRepository.findById(driverId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보가 없습니다."));
		DriverStatus before = driver.getStatus();
		if (status == DriverStatus.INACTIVE && before != DriverStatus.INACTIVE) {
			suspend(driver);
		}
		driver.changeStatus(status); // 변경 감지로 저장 (트랜잭션이 끝날 때 UPDATE)

		log.info("기사 상태 변경: driverId={}, {} → {}", driverId, before, status);
		notifyStatusChange(driver, before, status);
		return entityToDTO(driver);
	}

	// 중지(가입 거절 포함) 전 처리
	// 1) 배정 ~ 도착 중인 배송이 있으면 막는다 — 관리자가 배차 취소(재배차)하거나 운송이 끝난 뒤 중지
	//    (배송완료 DELIVERED는 고객 수령 확인만 남아 기사가 할 일이 없으므로 막지 않는다)
	// 2) 기사의 대기 중 지원은 자동 마감(NOT_SELECTED) — 고객이 고르면 그때 "운송할 수 없는 기사" 오류가 나던 것
	private void suspend(Driver driver) {
		long ongoing = dispatchRepository.countByDriver_IdAndActiveTrueAndShipment_StatusIn(
				driver.getId(), ONGOING_STATUSES);
		if (ongoing > 0) {
			throw new CustomException(ErrorCode.DRIVER_HAS_ACTIVE_SHIPMENT,
					"배정되었거나 운송 중인 배송이 " + ongoing + "건 있어 중지할 수 없어요. 배차를 취소(재배차)하거나 운송이 끝난 뒤 중지해 주세요.");
		}
		List<DispatchApplication> pending = applicationRepository.findByDriver_IdAndStatus(
				driver.getId(), DispatchApplicationStatus.PENDING);
		pending.forEach(DispatchApplication::notSelect);
		if (!pending.isEmpty()) {
			log.info("기사 중지로 대기 지원 자동 마감: driverId={}, {}건", driver.getId(), pending.size());
		}
	}

	// 기사에게 승인 · 가입 거절 · 중지 · 다시 활성화 알림 (상태가 그대로면 보내지 않는다)
	// 승인 대기 · 중지 중에는 로그인할 수 없어서, 다음에 로그인했을 때 알림 목록에서 보인다
	private void notifyStatusChange(Driver driver, DriverStatus before, DriverStatus after) {
		if (before == after) {
			return;
		}
		String message;
		if (after == DriverStatus.ACTIVE) {
			message = before == DriverStatus.PENDING
					? "기사 가입이 승인되었어요. 이제 운송 요청에 지원할 수 있어요."
					: "기사 계정이 다시 활성화되었어요. 운송 요청에 다시 지원할 수 있어요.";
		} else {
			message = before == DriverStatus.PENDING
					? "기사 가입이 승인되지 않았어요."
					: "관리자가 기사 계정을 중지했어요.";
		}
		notificationService.send(driver.getMember(), NotificationType.DRIVER_STATUS_CHANGED, message, null);
	}

	// 기사 ID로 기사 정보 조회
	@Override
	public Optional<Driver> findById(Long id) {
		return driverRepository.findById(id);
	}

	// 회원 ID로 기사 정보 조회 (위치 · 리뷰 서비스가 저장소 대신 쓰는 약속 메서드)
	@Override
	public Optional<Driver> findByMemberId(Long memberId) {
		return driverRepository.findByMemberId(memberId);
	}
}
