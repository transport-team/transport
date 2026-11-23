package com.transport.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.transport.domain.Driver;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleDeletionRequest;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.VehicleRegistrationStatus;
import com.transport.domain.enums.VehicleStatus;
import com.transport.dto.VehicleDeletionResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.VehicleDeletionRequestRepository;
import com.transport.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleDeletionServiceImpl implements VehicleDeletionService {
    private final VehicleDeletionRequestRepository requestRepository;
    private final VehicleRepository vehicleRepository;
    private final DriverRepository driverRepository;
    private final DispatchRepository dispatchRepository;
    private final DispatchApplicationRepository applicationRepository;

    @Override
    @Transactional
    public Long apply(Long memberId, Long vehicleId) {
        Driver driver = driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다."));
        if (driver.getStatus() != DriverStatus.ACTIVE) {
            throw new CustomException(ErrorCode.FORBIDDEN, "승인된 기사만 삭제를 신청할 수 있습니다.");
        }
        // 동일 차량을 동시 신청하는 경우도 잠금 후 재검사
        Vehicle vehicle = vehicleRepository.findForUpdate(vehicleId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "차량을 찾을 수 없습니다."));
        if (vehicle.getDriver() == null || !vehicle.getDriver().getId().equals(driver.getId())) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인 소유 차량만 삭제 신청할 수 있습니다.");
        }
        if (vehicle.getStatus() == VehicleStatus.INACTIVE) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미 비활성화된 차량입니다.");
        }
        if (requestRepository.existsByVehicle_IdAndStatus(vehicleId, VehicleRegistrationStatus.PENDING)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미 삭제 신청이 진행 중인 차량입니다.");
        }
        ensureNoOpenAssignment(vehicleId);
        VehicleStatus previous = vehicle.getStatus();
        vehicle.changeStatus(VehicleStatus.MAINTENANCE);
        VehicleDeletionRequest request = VehicleDeletionRequest.builder()
                .driver(driver).vehicle(vehicle).previousStatus(previous)
                .status(VehicleRegistrationStatus.PENDING).build();
        return requestRepository.save(request).getId();
    }

    @Override
    public List<VehicleDeletionResponseDTO> getMyRequests(Long memberId) {
        return requestRepository.findByDriver_Member_IdOrderByCreatedAtDescIdDesc(memberId)
                .stream().map(VehicleDeletionResponseDTO::from).toList();
    }

    @Override
    public List<VehicleDeletionResponseDTO> getAllRequests() {
        return requestRepository.findAllByOrderByCreatedAtDescIdDesc()
                .stream().map(VehicleDeletionResponseDTO::from).toList();
    }

    @Override
    @Transactional
    public void withdraw(Long memberId, Long requestId) {
        VehicleDeletionRequest request = getPending(requestId);
        if (!request.getDriver().getMember().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인 삭제 신청만 철회할 수 있습니다.");
        }
        restorePreviousState(request);
        request.withdraw();
    }

    @Override
    @Transactional
    public void approve(Long requestId) {
        VehicleDeletionRequest request = getPending(requestId);
        Vehicle vehicle = vehicleRepository.findForUpdate(request.getVehicle().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "차량을 찾을 수 없습니다."));
        ensureNoOpenAssignment(vehicle.getId());
        if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "차량 상태가 변경되어 삭제 승인을 진행할 수 없습니다.");
        }
        vehicle.changeStatus(VehicleStatus.INACTIVE);
        request.approve();
    }

    @Override
    @Transactional
    public void reject(Long requestId, String reason) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 300) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "거절 사유를 1~300자로 입력해 주세요.");
        }
        VehicleDeletionRequest request = getPending(requestId);
        restorePreviousState(request);
        request.reject(reason.trim());
    }

    private VehicleDeletionRequest getPending(Long id) {
        VehicleDeletionRequest request = requestRepository.findForUpdate(id)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "차량 삭제 신청을 찾을 수 없습니다."));
        if (request.getStatus() != VehicleRegistrationStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미 처리된 차량 삭제 신청입니다.");
        }
        return request;
    }

    private void restorePreviousState(VehicleDeletionRequest request) {
        Vehicle vehicle = vehicleRepository.findForUpdate(request.getVehicle().getId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "차량을 찾을 수 없습니다."));
        if (vehicle.getStatus() != VehicleStatus.MAINTENANCE) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "차량 상태가 달라져 복구할 수 없습니다.");
        }
        vehicle.changeStatus(request.getPreviousStatus());
    }

    private void ensureNoOpenAssignment(Long vehicleId) {
        if (dispatchRepository.existsByVehicle_IdAndActiveTrue(vehicleId)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "현재 활성 배차가 있어 차량 삭제를 진행할 수 없습니다.");
        }
        if (applicationRepository.existsByVehicle_IdAndStatus(vehicleId, DispatchApplicationStatus.PENDING)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "대기 중인 배차 신청을 먼저 취소해 주세요.");
        }
    }
}
