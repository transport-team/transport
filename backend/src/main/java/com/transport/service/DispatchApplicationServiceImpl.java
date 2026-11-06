package com.transport.service;

import java.time.LocalDateTime;
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
import com.transport.domain.enums.ShipmentStatus;
import com.transport.domain.enums.VehicleStatus;
import com.transport.dto.DispatchApplicantResponseDTO;
import com.transport.dto.DriverShipmentListResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.ShipmentRepository;
import com.transport.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchApplicationServiceImpl implements DispatchApplicationService {

    private final DispatchApplicationRepository applicationRepository;
    private final DriverRepository driverRepository;
    private final ShipmentRepository shipmentRepository;
    private final VehicleRepository vehicleRepository;
    private final DispatchRepository dispatchRepository;

    // 기사: 운송 요청에 지원
    @Override
    @Transactional
    public Long apply(Long memberId, Long shipmentId, Long vehicleId) {

        // 1. 로그인한 회원의 기사 정보 조회
        Driver driver = driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));

        // 2. 승인된 기사인지 확인
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN, "승인된 기사만 지원할 수 있습니다.");
        }

        // 3. 지원할 배송 요청 조회
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

        // 4. 배차 대기 중인 배송인지 확인
        if (shipment.getStatus() != ShipmentStatus.REQUESTED) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT, "현재 지원할 수 없는 운송 요청입니다.");
        }

        // 5. 지원할 차량 조회
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND, "차량을 찾을 수 없습니다."));

        // 6. 본인 소유 차량인지 확인
        if (vehicle.getDriver() == null ||
                !vehicle.getDriver().getId().equals(driver.getId())) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN, "본인 차량으로만 지원할 수 있습니다.");
        }

        // 7. 운행 가능한 차량인지 확인
        if (vehicle.getStatus() != VehicleStatus.AVAILABLE) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT, "현재 운행할 수 없는 차량입니다.");
        }

        // 8. 고객이 요청한 차종과 일치하는지 확인
        if (!vehicle.getVehicleType().getId()
                .equals(shipment.getVehicleType().getId())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT, "요청한 차종과 일치하지 않습니다.");
        }

        // 9. 동일 배송에 이미 지원 대기 중인지 확인
        if (applicationRepository.existsByShipmentAndDriverAndStatus(
                shipment, driver, DispatchApplicationStatus.PENDING)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT, "이미 지원한 운송 요청입니다.");
        }

        // 10. 지원 내역 생성
        DispatchApplication application = DispatchApplication.builder()
                .shipment(shipment)
                .driver(driver)
                .vehicle(vehicle)
                .status(DispatchApplicationStatus.PENDING)
                .build();

        // 11. DB에 저장 후 지원 ID 반환
        return applicationRepository.save(application).getId();
    }

    // 고객이 자신의 배송에 지원한 기사 목록 조회
    @Override
    public List<DispatchApplicantResponseDTO> getApplicants(Long memberId, Long shipmentId) {

        // 1. 배송 조회
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

        // 2. 로그인한 고객이 해당 배송의 신청자인지 확인
        if (!shipment.getCustomer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 3. 해당 배송에 지원한 기사 중 PENDING 상태만 조회
        return applicationRepository.findByShipmentAndStatusOrderByCreatedAtAsc(
                        shipment, DispatchApplicationStatus.PENDING)
                .stream()
                .map(application -> DispatchApplicantResponseDTO.builder()
                        .applicationId(application.getId())
                        .driverId(application.getDriver().getId())
                        .licenseType(application.getDriver().getLicenseType())
                        .careerYears(application.getDriver().getCareerYears())
                        .vehicleId(application.getVehicle().getId())
                        .plateNo(application.getVehicle().getPlateNo())
                        .vehicleTypeId(application.getVehicle().getVehicleType().getId())
                        .build())
                .toList();
    }

    // 고객이 지원 기사 한 명을 선택하여 배차 확정
    @Override
    @Transactional
    public Long selectApplicant(Long memberId, Long shipmentId, Long applicationId) {

        // 1. 배송 조회
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

        // 2. 본인이 등록한 배송인지 확인
        if (!shipment.getCustomer().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN);
        }

        // 3. 배차 대기 중인 배송인지 확인
        if (shipment.getStatus() != ShipmentStatus.REQUESTED) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 4. 이미 확정된 배차가 있는지 확인
        if (dispatchRepository.existsByShipmentAndActiveTrue(shipment)) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 5. 고객이 선택한 지원 신청 조회
        DispatchApplication selectedApplication = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 6. 해당 배송에 들어온 PENDING 지원인지 확인
        if (!selectedApplication.getShipment().getId().equals(shipmentId)
                || selectedApplication.getStatus() != DispatchApplicationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        // 7. 해당 배송의 대기 중인 지원 신청 전체 조회
        List<DispatchApplication> pendingApplications =
                applicationRepository.findByShipmentAndStatusOrderByCreatedAtAsc(
                        shipment, DispatchApplicationStatus.PENDING);

        // 8. 확정 배차 기록 생성
        Dispatch dispatch = Dispatch.builder()
                .shipment(shipment)
                .driver(selectedApplication.getDriver())
                .vehicle(selectedApplication.getVehicle())
                .assignedBy(shipment.getCustomer())
                .active(true)
                .assignedAt(LocalDateTime.now())
                .build();

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
        shipment.updateStatus(ShipmentStatus.ASSIGNED);

        return dispatch.getId();
    }

	// 기사: 배차 대기 중인 운송 요청 목록 조회
	@Override
	public List<DriverShipmentListResponseDTO> getAvailableShipments(Long memberId) {

		// 1. REQUESTED 상태의 배송을 상차 예정일 빠른 순으로 조회
		List<Shipment> shipments = shipmentRepository
				.findByStatusOrderByPickupAtAsc(ShipmentStatus.REQUESTED);

		// 2. Shipment 엔티티를 기사 목록용 DTO로 변환
		return shipments.stream()
				.map(shipment -> DriverShipmentListResponseDTO.builder()
						.shipmentId(shipment.getId())
						.originAddress(shipment.getOriginAddress())
						.destAddress(shipment.getDestAddress())
						.cargoName(shipment.getCargoName())
						.cargoWeightKg(shipment.getCargoWeightKg())
						.vehicleTypeId(shipment.getVehicleType().getId())
						.fare(shipment.getFare())
						.pickupAt(shipment.getPickupAt())
						.build())
				.toList();
	}


}
