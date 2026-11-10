package com.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Driver;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleRegistrationRequest;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.VehicleRegistrationStatus;
import com.transport.domain.enums.VehicleStatus;
import com.transport.dto.DriverVehicleResponseDTO;
import com.transport.dto.VehicleRegistrationRequestDTO;
import com.transport.dto.VehicleRegistrationResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.VehicleRegistrationRequestRepository;
import com.transport.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleRegistrationServiceImpl implements VehicleRegistrationService {

    private final VehicleRegistrationRequestRepository requestRepository;
    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;
    private final VehicleTypeService vehicleTypeService;
    private final DispatchRepository dispatchRepository;

    @Override
    @Transactional
    public Long apply(Long memberId, VehicleRegistrationRequestDTO requestDTO) {

        // 1. 로그인한 회원의 기사 정보 조회
        Driver driver = driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));

        // 2. 승인된 기사인지 확인
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new CustomException(ErrorCode.FORBIDDEN, "승인된 기사만 차량 등록을 신청할 수 있습니다.");
        }

        // 3. 신청한 차종 조회
        VehicleType vehicleType = vehicleTypeService.findById(requestDTO.getVehicleTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "차종을 찾을 수 없습니다."));

        if (!vehicleType.isActive()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "현재 등록할 수 없는 차종입니다.");
        }

        // 4. 이미 등록된 차량 또는 승인 대기 신청의 차종 중복 확인
        if (vehicleRepository.existsByDriverAndVehicleType(driver, vehicleType)
                || requestRepository.existsByDriverAndVehicleTypeAndStatus(
                        driver, vehicleType, VehicleRegistrationStatus.PENDING)) {

            throw new CustomException(ErrorCode.DUPLICATE_DRIVER_VEHICLE_TYPE);
        }

        // 5. 차량번호 중복 확인
        if (vehicleRepository.existsByPlateNo(requestDTO.getPlateNo())
                || requestRepository.existsByPlateNoAndStatus(
                        requestDTO.getPlateNo(), VehicleRegistrationStatus.PENDING)) {

            throw new CustomException(
                    ErrorCode.DUPLICATE_VEHICLE_PLATE,
                    "이미 등록되었거나 승인 대기 중인 차량번호입니다.");
        }

        // 6. 차량 등록 신청 생성 (아직 실제 차량은 등록하지 않음)
        VehicleRegistrationRequest request = VehicleRegistrationRequest.builder()
                .driver(driver)
                .vehicleType(vehicleType)
                .plateNo(requestDTO.getPlateNo())
                .status(VehicleRegistrationStatus.PENDING)
                .build();

        // 7. 신청 정보 저장 후 신청 ID 반환
        return requestRepository.save(request).getId();
    }
    
    // 관리자: 승인 대기 중인 차량 등록 신청 목록 조회
    @Override
    public List<VehicleRegistrationResponseDTO> getPendingRequests() {

        return requestRepository
                .findByStatusOrderByCreatedAtDesc(VehicleRegistrationStatus.PENDING)
                .stream()
                .map(request -> VehicleRegistrationResponseDTO.builder()
                        .id(request.getId())
                        .driverId(request.getDriver().getId())
                        .driverName(request.getDriver().getMember().getName())
                        .plateNo(request.getPlateNo())
                        .vehicleTypeId(request.getVehicleType().getId())
                        .vehicleTypeName(request.getVehicleType().getName())
                        .status(request.getStatus())
                        .rejectionReason(request.getRejectionReason())
                        .reviewedAt(request.getReviewedAt())
                        .createdAt(request.getCreatedAt())
                        .build())
                .toList();
    }
    
    // 관리자: 차량 등록 신청 승인
    @Override
    @Transactional
    public void approve(Long requestId) {

        // 1. 신청 정보 조회
        VehicleRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 2. 승인 대기 중인 신청인지 확인
        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미 처리된 차량 등록 신청입니다.");
        }

        Driver driver = request.getDriver();
        VehicleType vehicleType = request.getVehicleType();

        // 3. 기사와 차종 상태 확인
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new CustomException(ErrorCode.FORBIDDEN, "활성 상태의 기사만 차량을 등록할 수 있습니다.");
        }

        if (!vehicleType.isActive()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "현재 등록할 수 없는 차종입니다.");
        }

        // 4. 승인 시점에 중복 검사
        if (vehicleRepository.existsByDriverAndVehicleType(driver, vehicleType)) {
            throw new CustomException(ErrorCode.DUPLICATE_DRIVER_VEHICLE_TYPE);
        }

        if (vehicleRepository.existsByPlateNo(request.getPlateNo())) {
            throw new CustomException(ErrorCode.DUPLICATE_VEHICLE_PLATE);
        }

        // 5. 실제 차량 등록
        Vehicle vehicle = Vehicle.builder()
                .plateNo(request.getPlateNo())
                .vehicleType(vehicleType)
                .driver(driver)
                .status(VehicleStatus.AVAILABLE)
                .build();

        vehicleRepository.save(vehicle);

        // 6. 신청 상태를 승인 완료로 변경
        request.approve();
    }

    // 관리자: 차량 등록 신청 거절
    @Override
    @Transactional
    public void reject(Long requestId, String rejectionReason) {

        // 1. 신청 정보 조회
        VehicleRegistrationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 2. 승인 대기 중인 신청인지 확인
        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미 처리된 차량 등록 신청입니다.");
        }

        // 3. 거절 사유 검증
        if (rejectionReason == null || rejectionReason.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "거절 사유를 입력해 주세요.");
        }

        if (rejectionReason.trim().length() > 300) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "거절 사유는 300자 이내로 입력해 주세요.");
        }

        // 4. 신청 상태를 거절로 변경
        request.reject(rejectionReason.trim());
    }

    // 기사: 본인 차량 목록 조회
    @Override
    public List<DriverVehicleResponseDTO> getMyVehicles(Long memberId) {

        // 1. 로그인한 회원의 기사 정보 조회
        Driver driver = driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));

        // 2. 기사 소유 차량 목록 조회 후 DTO로 변환
        return vehicleRepository.findByDriver_Id(driver.getId())
                .stream()
                .map(vehicle -> DriverVehicleResponseDTO.builder()
                        .vehicleId(vehicle.getId())
                        .plateNo(vehicle.getPlateNo())
                        .vehicleTypeId(vehicle.getVehicleType().getId())
                        .status(vehicle.getStatus())
                        .build())
                .toList();
    }

    // 기사: 본인 차량 운행 상태 변경
    @Override
    @Transactional
    public DriverVehicleResponseDTO changeMyVehicleStatus(
            Long memberId, Long vehicleId, VehicleStatus status) {

        // 1. 로그인한 회원의 기사 정보 조회
        Driver driver = driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));

        // 2. 승인된 기사인지 확인
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN, "승인된 기사만 차량 상태를 변경할 수 있습니다.");
        }

        // 3. 변경할 차량 조회
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND, "차량을 찾을 수 없습니다."));

        // 4. 본인 차량인지 확인
        if (vehicle.getDriver() == null
                || !vehicle.getDriver().getId().equals(driver.getId())) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN, "본인 차량만 상태를 변경할 수 있습니다.");
        }

        // 5. 운행 가능 / 정비 중 상태만 허용
        if (status != VehicleStatus.AVAILABLE
                && status != VehicleStatus.MAINTENANCE) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "운행 가능 또는 정비 중 상태로만 변경할 수 있습니다.");
        }

        // 6. 비활성화된 차량은 기사 본인이 변경할 수 없음
        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            throw new CustomException(
                    ErrorCode.FORBIDDEN,
                    "비활성화된 차량은 상태를 변경할 수 없습니다.");
        }

        // 7. 이미 같은 상태인지 확인
        if (vehicle.getStatus() == status) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT, "이미 해당 상태인 차량입니다.");
        }

        // 8. 유효한 배차가 있는 차량은 정비 중 변경 불가
        if (status == VehicleStatus.MAINTENANCE
                && dispatchRepository.existsByVehicle_IdAndActiveTrue(vehicleId)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "현재 유효한 배차가 있어 정비 중으로 변경할 수 없습니다.");
        }

        // 9. 차량 상태 변경
        vehicle.changeStatus(status);

        // 10. 변경된 차량 정보 반환
        return DriverVehicleResponseDTO.builder()
                .vehicleId(vehicle.getId())
                .plateNo(vehicle.getPlateNo())
                .vehicleTypeId(vehicle.getVehicleType().getId())
                .status(vehicle.getStatus())
                .build();
    }
}