package com.transport.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Dispatch;
import com.transport.domain.DispatchApplication;
import com.transport.domain.Driver;
import com.transport.domain.Shipment;
import com.transport.domain.Vehicle;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.domain.enums.VehicleStatus;
import com.transport.dto.DispatchApplicantResponseDTO;
import com.transport.dto.DriverShipmentListResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.DriverScheduleRepository;
import com.transport.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchApplicationServiceImpl implements DispatchApplicationService {

	private final DispatchApplicationRepository applicationRepository;
	private final DriverRepository driverRepository;
	private final ShipmentService shipmentService;
	private final VehicleRepository vehicleRepository;
	private final DispatchRepository dispatchRepository;
	private final DriverScheduleRepository driverScheduleRepository;
	// 배송 상태 변경 이력 저장 서비스
	private final StatusHistoryService statusHistoryService;
	// 배차 확정 알림 서비스
	private final NotificationService notificationService;

	// 기사: 운송 요청에 지원
	@Override
	@Transactional
	public Long apply(Long memberId, Long shipmentId, Long vehicleId) {

		// 1. 로그인한 회원의 기사 정보 조회
		Driver driver = driverRepository.findByMemberId(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));

		// 2. 승인된 기사인지 확인
		if (driver.getStatus() != DriverStatus.ACTIVE) {
			throw new CustomException(ErrorCode.FORBIDDEN, "승인된 기사만 지원할 수 있습니다.");
		}

		// 3. 지원할 배송 요청 조회
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 4. 배차 대기 중인 배송인지 확인
		if (shipment.getStatus() != ShipmentStatus.REQUESTED) {
			throw new CustomException(
					ErrorCode.SHIPMENT_NOT_REQUESTED, "현재 지원할 수 없는 운송 요청입니다.");
		}

		// 5. 지원할 차량 조회
		Vehicle vehicle = vehicleRepository.findById(vehicleId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "차량을 찾을 수 없습니다."));

		// 6. 본인 소유 차량인지 확인
		if (vehicle.getDriver() == null || !vehicle.getDriver().getId().equals(driver.getId())) {
			throw new CustomException(ErrorCode.FORBIDDEN, "본인 차량으로만 지원할 수 있습니다.");
		}

		// 7. 운행 가능한 차량인지 확인
		if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "현재 운행할 수 없는 차량입니다.");
		}

		// 8. 고객이 요청한 차종과 일치하는지 확인
		if (!vehicle.getVehicleType().getId().equals(shipment.getVehicleType().getId())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "요청한 차종과 일치하지 않습니다.");
		}

		// 9. 동일 배송에 이미 지원 대기 중인지 확인
		if (applicationRepository.existsByShipmentAndDriverAndStatus(shipment, driver,
				DispatchApplicationStatus.PENDING)) {
			throw new CustomException(
					ErrorCode.DUPLICATE_DISPATCH_APPLICATION, "이미 지원한 운송 요청입니다.");
		}

		// 기사 휴무일에는 배차 신청 불가
		driverScheduleRepository.findByDriver_IdAndWorkDate(driver.getId(), shipment.getPickupAt().toLocalDate())
				.ifPresent(schedule -> {

					if ("OFF".equals(schedule.getStatus().name())) {
						throw new CustomException(ErrorCode.INVALID_INPUT, "상차 예정일이 기사 휴무일입니다.");
					}
				});

		// 예상 운송 시간이 없으면 배차 신청 불가
		if (isInvalidDuration(shipment)) {
			throw new CustomException(
					ErrorCode.INVALID_INPUT, "예상 운송 시간이 제공되지 않은 배송은 배차를 신청할 수 없습니다.");
		}

		// 새 운송 요청의 예상 시작 및 종료 시간
		LocalDateTime requestedStart = shipment.getPickupAt();
		LocalDateTime requestedEnd = requestedStart.plusMinutes(durationOrZero(shipment));

		// 이미 확정된 배차와 운송 시간이 겹치는지 확인
		boolean hasOverlappingDispatch = dispatchRepository.findByDriver_IdAndActiveTrue(driver.getId()).stream()
				.anyMatch(dispatch -> {

					Shipment assignedShipment = dispatch.getShipment();

					if (isInvalidDuration(assignedShipment)) {
						return false;
					}

					LocalDateTime assignedStart = assignedShipment.getPickupAt();
					LocalDateTime assignedEnd = assignedStart.plusMinutes(durationOrZero(assignedShipment));

					return requestedStart.isBefore(assignedEnd) && assignedStart.isBefore(requestedEnd);
				});

		if (hasOverlappingDispatch) {
			throw new CustomException(
					ErrorCode.DISPATCH_TIME_CONFLICT, "이미 확정된 배차와 운송 시간이 겹칩니다.");
		}

		// 10. 지원 내역 생성
		DispatchApplication application = DispatchApplication.builder().shipment(shipment).driver(driver)
				.vehicle(vehicle).status(DispatchApplicationStatus.PENDING).build();

		// 11. DB에 저장 후 지원 ID 반환
		return applicationRepository.save(application).getId();
	}

	// 기사: 배차 확정 전 신청 취소
	@Override
	@Transactional
	public void withdrawApplication(Long memberId, Long shipmentId) {

		// 1. 로그인한 기사 조회
		Driver driver = driverRepository.findByMemberId(memberId)
				.orElseThrow(() -> new CustomException(
						ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));

		// 2. 운송 요청 조회
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 3. 배차 대기 중일 때만 취소 가능
		if (shipment.getStatus() != ShipmentStatus.REQUESTED) {
			throw new CustomException(
					ErrorCode.SHIPMENT_NOT_REQUESTED,
					"배차가 확정되었거나 종료된 요청은 신청을 취소할 수 없습니다.");
		}

		// 4. 로그인한 기사의 대기 중 신청 찾기
		DispatchApplication application = applicationRepository
				.findByDriver_IdAndStatus(
						driver.getId(),
						DispatchApplicationStatus.PENDING)
				.stream()
				.filter(item -> item.getShipment().getId().equals(shipmentId))
				.findFirst()
				.orElseThrow(() -> new CustomException(
						ErrorCode.DISPATCH_APPLICATION_NOT_PENDING,
						"취소할 수 있는 배차 신청이 없습니다."));

		// 5. 신청 기록은 유지하고 철회 상태로 변경
		application.withdraw();
	}

	// 고객이 자신의 배송에 지원한 기사 목록 조회
	@Override
	public List<DispatchApplicantResponseDTO> getApplicants(Long memberId, Long shipmentId) {

		// 1. 배송 조회
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 2. 로그인한 고객이 해당 배송의 신청자인지 확인
		if (!shipment.getCustomer().getId().equals(memberId)) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}

		// 3. 해당 배송에 지원한 기사 중 PENDING 상태만 조회
		return applicationRepository
				.findByShipmentAndStatusOrderByCreatedAtAsc(shipment, DispatchApplicationStatus.PENDING).stream()
				.map(application -> DispatchApplicantResponseDTO.builder().applicationId(application.getId())
						.driverId(application.getDriver().getId()).licenseType(application.getDriver().getLicenseType())
						.careerYears(application.getDriver().getCareerYears())
						.vehicleId(application.getVehicle().getId()).plateNo(application.getVehicle().getPlateNo())
						.vehicleTypeId(application.getVehicle().getVehicleType().getId()).build())
				.toList();
	}

	// 고객이 지원 기사 한 명을 선택하여 배차 확정
	@Override
	@Transactional
	public Long selectApplicant(Long memberId, Long shipmentId, Long applicationId) {

		// 1. 배송 조회
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 2. 본인이 등록한 배송인지 확인
		if (!shipment.getCustomer().getId().equals(memberId)) {
			throw new CustomException(ErrorCode.FORBIDDEN);
		}

		// 3. 배차 대기 중인 배송인지 확인
		if (shipment.getStatus() != ShipmentStatus.REQUESTED) {
			throw new CustomException(ErrorCode.SHIPMENT_NOT_REQUESTED);
		}

		// 4. 이미 확정된 배차가 있는지 확인
		if (dispatchRepository.existsByShipmentAndActiveTrue(shipment)) {
			throw new CustomException(ErrorCode.INVALID_INPUT);
		}

		// 5. 고객이 선택한 지원 신청 조회
		DispatchApplication selectedApplication = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

		// 6. 해당 배송에 들어온 PENDING 지원인지 확인
		if (!selectedApplication.getShipment().getId().equals(shipmentId)) {
			throw new CustomException(
					ErrorCode.INVALID_INPUT, "해당 운송 요청의 배차 신청이 아닙니다.");
		}

		if (selectedApplication.getStatus() != DispatchApplicationStatus.PENDING) {
			throw new CustomException(
					ErrorCode.DISPATCH_APPLICATION_NOT_PENDING);
		}

		// 배차 확정 전 예상 운송 시간 검증
		if (isInvalidDuration(shipment)) {
			throw new CustomException(
					ErrorCode.INVALID_INPUT, "예상 운송 시간이 제공되지 않은 배송은 배차를 확정할 수 없습니다.");
		}

		// 선택한 기사의 기존 확정 배차와 시간 중복 확인
		LocalDateTime requestedStart = shipment.getPickupAt();
		LocalDateTime requestedEnd = requestedStart.plusMinutes(durationOrZero(shipment));

		boolean hasOverlappingDispatch = dispatchRepository
				.findByDriver_IdAndActiveTrue(selectedApplication.getDriver().getId()).stream().anyMatch(dispatch -> {

					Shipment assignedShipment = dispatch.getShipment();

					if (isInvalidDuration(assignedShipment)) {
						return false;
					}

					LocalDateTime assignedStart = assignedShipment.getPickupAt();
					LocalDateTime assignedEnd = assignedStart.plusMinutes(durationOrZero(assignedShipment));

					return requestedStart.isBefore(assignedEnd) && assignedStart.isBefore(requestedEnd);
				});

		if (hasOverlappingDispatch) {
			throw new CustomException(
					ErrorCode.DISPATCH_TIME_CONFLICT,
					"선택한 기사에게 시간이 겹치는 확정 배차가 있습니다.");
		}

		// 배차 확정 시 기사 상태 재검증
		Driver selectedDriver = selectedApplication.getDriver();

		if (selectedDriver.getStatus() != DriverStatus.ACTIVE) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "현재 운송할 수 없는 기사입니다.");
		}

		// 배차 확정 시 차량 상태 및 소유 관계 재검증
		Vehicle selectedVehicle = selectedApplication.getVehicle();

		if (selectedVehicle.getDriver() == null
				|| !selectedVehicle.getDriver().getId().equals(selectedDriver.getId())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "기사 소유 차량이 아닙니다.");
		}

		if (selectedVehicle.getStatus() != VehicleStatus.AVAILABLE) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "현재 운행할 수 없는 차량입니다.");
		}

		// 고객이 요청한 차종과 여전히 일치하는지 확인
		if (!selectedVehicle.getVehicleType().getId().equals(shipment.getVehicleType().getId())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "요청한 차종과 일치하지 않습니다.");
		}

		// 배차 확정 시 기사 휴무일 재검증
		driverScheduleRepository
				.findByDriver_IdAndWorkDate(selectedDriver.getId(), shipment.getPickupAt().toLocalDate())
				.ifPresent(schedule -> {

					if ("OFF".equals(schedule.getStatus().name())) {
						throw new CustomException(ErrorCode.INVALID_INPUT, "선택한 기사의 상차 예정일이 휴무일입니다.");
					}
				});

		// 7. 해당 배송의 대기 중인 지원 신청 전체 조회
		List<DispatchApplication> pendingApplications = applicationRepository
				.findByShipmentAndStatusOrderByCreatedAtAsc(shipment, DispatchApplicationStatus.PENDING);

		// 8. 확정 배차 기록 생성
		Dispatch dispatch = Dispatch.builder().shipment(shipment).driver(selectedApplication.getDriver())
				.vehicle(selectedApplication.getVehicle()).assignedBy(shipment.getCustomer()).active(true)
				.assignedAt(LocalDateTime.now()).build();

		dispatchRepository.save(dispatch);

		// 9. 선택된 기사 지원 상태 변경
		selectedApplication.select();

		// 10. 선택되지 않은 기사 지원 상태 변경
		for (DispatchApplication application : pendingApplications) {
			if (!application.getId().equals(applicationId)) {
				application.notSelect();
			}
		}

		// 11. 배송 상태를 배차 완료로 변경
		// 공통 상태 전이 규칙을 검사한 후 ASSIGNED로 변경
		ShipmentStatus previousStatus = shipment.getStatus();
		shipment.changeStatus(ShipmentStatus.ASSIGNED);

		// 12. 배송 상태 변경 이력 저장
		statusHistoryService.record(
				shipment,
				previousStatus,
				ShipmentStatus.ASSIGNED,
				shipment.getCustomer(),
				"고객이 지원 기사를 선택하여 배차 확정");

		// 13. 선택된 기사에게 배차 확정 알림
		notificationService.send(
				selectedDriver.getMember(),
				NotificationType.DISPATCHED,
				"배송 " + shipment.getId() + "번의 배차가 확정되었습니다.",
				shipment);

		return dispatch.getId();

	}

	// 고객이 지원한 기사를 개별적으로 거절
	@Override
	@Transactional
	public void rejectApplicant(Long memberId, Long shipmentId, Long applicationId) {

		// 1. 배송 조회
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 2. 본인이 등록한 배송인지 확인
		if (!shipment.getCustomer().getId().equals(memberId)) {
			throw new CustomException(
					ErrorCode.FORBIDDEN, "본인의 운송 요청만 관리할 수 있습니다.");
		}

		// 3. 배차 대기 중인 배송인지 확인
		if (shipment.getStatus() != ShipmentStatus.REQUESTED) {
			throw new CustomException(ErrorCode.SHIPMENT_NOT_REQUESTED, "현재 지원을 거절할 수 없는 운송 요청입니다.");
		}

		// 4. 거절할 지원 신청 조회
		DispatchApplication application = applicationRepository.findById(applicationId)
				.orElseThrow(() -> new CustomException(
						ErrorCode.NOT_FOUND, "배차 신청을 찾을 수 없습니다."));

		// 5. 해당 배송의 대기 중인 지원인지 확인
		if (!application.getShipment().getId().equals(shipmentId)) {
			throw new CustomException(
					ErrorCode.INVALID_INPUT, "해당 운송 요청의 배차 신청이 아닙니다.");
		}

		if (application.getStatus() != DispatchApplicationStatus.PENDING) {
			throw new CustomException(
					ErrorCode.DISPATCH_APPLICATION_NOT_PENDING);
		}

		// 6. 지원 거절
		application.reject();
	}

	// 기사: 배차 대기 중인 운송 요청 목록 조회
	@Override
	public List<DriverShipmentListResponseDTO> getAvailableShipments(Long memberId) {

		// 1. 로그인한 기사 조회
		Driver driver = driverRepository.findByMemberId(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));

		// 2. 기사 소유 차량 목록 조회
		List<Vehicle> vehicles = vehicleRepository.findByDriver_Id(driver.getId());

		// 기사가 이미 확정받은 배차 목록 조회
		List<Dispatch> activeDispatches = dispatchRepository.findByDriver_IdAndActiveTrue(driver.getId());

		// 기사가 아직 확정받지 않은 배차 신청 목록 조회
		List<DispatchApplication> pendingApplications = applicationRepository.findByDriver_IdAndStatus(driver.getId(),
				DispatchApplicationStatus.PENDING);

		// 3. REQUESTED 상태의 배송 조회
		List<Shipment> shipments = shipmentService.getRequestedShipments();

		// 4. 배송별 신청 가능 조건 검사
		return shipments.stream().map(shipment -> {

			List<String> blockReasons = new ArrayList<>();
			List<String> warningMessages = new ArrayList<>();

			// 예상 운송 시간이 올바르지 않으면 배차 신청 불가
			if (isInvalidDuration(shipment)) {
				blockReasons.add("예상 운송 시간이 올바르지 않아 배차 신청할 수 없습니다.");
			}

			// 동일 운송 요청에 이미 배차 신청했는지 확인
			boolean alreadyApplied = pendingApplications.stream()
					.anyMatch(application -> application.getShipment().getId().equals(shipment.getId()));

			if (alreadyApplied) {
				blockReasons.add("이미 배차 신청한 운송 요청입니다.");
			}

			// 승인된 기사인지 확인
			if (driver.getStatus() != DriverStatus.ACTIVE) {
				blockReasons.add("승인된 기사만 배차 신청할 수 있습니다.");
			}

			// 요청 차종의 차량을 보유하고 있는지 확인
			boolean hasMatchingVehicle = vehicles.stream()
					.anyMatch(vehicle -> vehicle.getVehicleType().getId().equals(shipment.getVehicleType().getId()));

			// 요청 차종의 운행 가능한 차량이 있는지 확인
			boolean hasAvailableVehicle = vehicles.stream()
					.anyMatch(vehicle -> vehicle.getVehicleType().getId().equals(shipment.getVehicleType().getId())
							&& vehicle.getStatus() == VehicleStatus.AVAILABLE);

			if (!hasMatchingVehicle) {
				blockReasons.add("요청한 차종의 차량을 보유하고 있지 않습니다.");
			} else if (!hasAvailableVehicle) {
				blockReasons.add("요청한 차종의 차량이 운행 가능한 상태가 아닙니다.");
			}

			// 기사 근무 일정 확인 (상차 예정일 기준)
			driverScheduleRepository.findByDriver_IdAndWorkDate(driver.getId(), shipment.getPickupAt().toLocalDate())
					.ifPresentOrElse(schedule -> {

						// 휴무일이면 배차 신청 불가
						if ("OFF".equals(schedule.getStatus().name())) {
							blockReasons.add("상차 예정일이 기사 휴무일입니다.");

						} else if (schedule.getStartTime() != null && schedule.getEndTime() != null) {

							// 기사 근무 종료 일시
							LocalDateTime workEnd = LocalDateTime.of(schedule.getWorkDate(), schedule.getEndTime());

							// 자정을 넘기는 근무시간 처리
							if (schedule.getEndTime().isBefore(schedule.getStartTime())) {
								workEnd = workEnd.plusDays(1);
							}

							// 운송 요청의 예상 종료 일시
							LocalDateTime deliveryEnd = shipment.getPickupAt().plusMinutes(durationOrZero(shipment));

							// 근무 종료 시간을 초과하면 경고만 표시
							if (deliveryEnd.isAfter(workEnd)) {
								warningMessages.add("예상 운송 종료 시간이 근무 종료 시간을 초과합니다.");
							}

						} else {
							warningMessages.add("등록된 근무시간 정보가 부족합니다.");
						}

					}, () -> {
						// 해당 날짜에 근무 일정이 없는 경우
						warningMessages.add("상차 예정일의 근무 일정이 등록되지 않았습니다.");
					});

			// 새 운송 요청의 예상 시작 및 종료 시간
			LocalDateTime requestedStart = shipment.getPickupAt();
			LocalDateTime requestedEnd = requestedStart.plusMinutes(durationOrZero(shipment));

			// 기존 확정 배차와 운송 시간이 겹치는지 확인
			boolean hasOverlappingDispatch = activeDispatches.stream().anyMatch(dispatch -> {

				Shipment assignedShipment = dispatch.getShipment();

				if (isInvalidDuration(assignedShipment)) {
					return false;
				}

				LocalDateTime assignedStart = assignedShipment.getPickupAt();
				LocalDateTime assignedEnd = assignedStart.plusMinutes(durationOrZero(assignedShipment));

				// 두 운송 시간 구간이 겹치는지 검사
				return requestedStart.isBefore(assignedEnd) && assignedStart.isBefore(requestedEnd);
			});

			if (hasOverlappingDispatch) {
				blockReasons.add("이미 확정된 배차와 운송 시간이 겹칩니다.");
			}

			// 다른 운송 요청의 대기 중인 배차 신청과 시간이 겹치는지 확인
			boolean hasOverlappingApplication = pendingApplications.stream()
					.filter(application -> !application.getShipment().getId().equals(shipment.getId()))
					.anyMatch(application -> {

						Shipment appliedShipment = application.getShipment();

						if (isInvalidDuration(appliedShipment)) {
							return false;
						}

						LocalDateTime appliedStart = appliedShipment.getPickupAt();
						LocalDateTime appliedEnd = appliedStart.plusMinutes(durationOrZero(appliedShipment));

						return requestedStart.isBefore(appliedEnd) && appliedStart.isBefore(requestedEnd);
					});

			if (hasOverlappingApplication) {
				warningMessages.add("다른 배차 신청과 운송 시간이 겹칩니다.");
			}

			// 5. 목록 DTO 생성
			return DriverShipmentListResponseDTO.builder().shipmentId(shipment.getId())
					.originAddress(shipment.getOriginAddress()).destAddress(shipment.getDestAddress())
					.cargoName(shipment.getCargoName()).cargoWeightKg(shipment.getCargoWeightKg())
					.vehicleTypeId(shipment.getVehicleType().getId()).fare(shipment.getFare())
					.pickupAt(shipment.getPickupAt()).durationMin(shipment.getDurationMin())
					.canApply(blockReasons.isEmpty()).alreadyApplied(alreadyApplied)
					.blockReasons(blockReasons).warningMessages(warningMessages)
					.build();
		}).toList();
	}

	private boolean isInvalidDuration(Shipment shipment) {
		return shipment.getDurationMin() == null || shipment.getDurationMin() <= 0;
	}

	private long durationOrZero(Shipment shipment) {
		return shipment.getDurationMin() == null ? 0L : shipment.getDurationMin();
	}
}
