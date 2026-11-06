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
import com.transport.dto.VehicleRegistrationRequestDTO;
import com.transport.dto.VehicleRegistrationResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
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
}