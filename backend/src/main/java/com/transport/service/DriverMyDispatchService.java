package com.transport.service;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Dispatch;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.domain.DispatchApplication;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.dto.DriverDispatchHistoryResponseDTO;
import com.transport.dto.DriverAssignedDispatchResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.ShipmentStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverMyDispatchService {

    private final DriverRepository driverRepository;
    private final DispatchRepository dispatchRepository;
    private final DispatchApplicationRepository applicationRepository;
    private final ShipmentStatusHistoryRepository statusHistoryRepository;

    // 배차 확정 후 배송 완료 전까지의 상태
    private static final Set<ShipmentStatus> ACTIVE_STATUSES = EnumSet.of(
            ShipmentStatus.ASSIGNED,
            ShipmentStatus.PICKED_UP,
            ShipmentStatus.IN_TRANSIT,
            ShipmentStatus.ARRIVED);

    // 기사: 본인의 진행 중인 확정 배차 목록 조회
    public List<DriverAssignedDispatchResponseDTO> getMyDispatches(Long memberId) {

        Long driverId = getDriverId(memberId);

        return dispatchRepository.findByDriver_IdAndActiveTrue(driverId)
                .stream()
                .filter(dispatch -> ACTIVE_STATUSES.contains(dispatch.getShipment().getStatus()))
                .sorted(Comparator.comparing(
                        dispatch -> dispatch.getShipment().getPickupAt()))
                .map(this::toResponse)
                .toList();
    }

    // 기사: 본인의 확정 배차 상세 조회
    public DriverAssignedDispatchResponseDTO getMyDispatch(
            Long memberId, Long shipmentId) {

        Long driverId = getDriverId(memberId);

        Dispatch dispatch = dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(shipmentId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "배차 정보를 찾을 수 없습니다."));

        // 다른 기사의 배차는 조회할 수 없음
        if (!dispatch.getDriver().getId().equals(driverId)) {
            throw new CustomException(
                    ErrorCode.NOT_FOUND,
                    "배차 정보를 찾을 수 없습니다.");
        }

        return toResponse(dispatch);
    }

    // 로그인 회원 ID로 기사 ID 조회
    private Long getDriverId(Long memberId) {
        return driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "기사 정보를 찾을 수 없습니다."))
                .getId();
    }

    // Dispatch 엔티티를 응답 DTO로 변환
    private DriverAssignedDispatchResponseDTO toResponse(Dispatch dispatch) {

        Shipment shipment = dispatch.getShipment();

        return DriverAssignedDispatchResponseDTO.builder()
                .dispatchId(dispatch.getId())
                .shipmentId(shipment.getId())
                .vehicleId(dispatch.getVehicle().getId())
                .vehicleTypeId(shipment.getVehicleType().getId())
                .cargoName(shipment.getCargoName())
                .cargoWeightKg(shipment.getCargoWeightKg())
                .fare(shipment.getFare())
                .originAddress(shipment.getOriginAddress())
                .originDetail(shipment.getOriginDetail())
                .destAddress(shipment.getDestAddress())
                .destDetail(shipment.getDestDetail())
                .pickupAt(shipment.getPickupAt())
                .durationMin(shipment.getDurationMin())
                .receiverName(shipment.getReceiverName())
                .receiverPhone(shipment.getReceiverPhone())
                .memo(shipment.getMemo())
                .status(shipment.getStatus().name())
                .assignedAt(dispatch.getAssignedAt())
                .build();
    }

    // 기사: 본인의 지난 신청 및 배송 내역 조회
    public List<DriverDispatchHistoryResponseDTO> getMyHistory(Long memberId) {

        Long driverId = getDriverId(memberId);

        return applicationRepository
                .findByDriver_IdOrderByUpdatedAtDesc(driverId)
                .stream()
                .filter(this::isHistory)
                .map(this::toHistoryResponse)
                // 배송 완료는 완료일, 나머지는 신청 기록 변경일 기준 최신순
                .sorted(Comparator.comparing(
                        (DriverDispatchHistoryResponseDTO history) -> history.getCompletedAt() != null
                                ? history.getCompletedAt()
                                : history.getUpdatedAt(),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    // 기사: 본인의 지난 신청 기록 상세 조회
    public DriverDispatchHistoryResponseDTO getMyHistoryDetail(
            Long memberId, Long applicationId) {

        Long driverId = getDriverId(memberId);

        DispatchApplication application = applicationRepository
                .findById(applicationId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "지난 배차 내역을 찾을 수 없습니다."));

        // 다른 기사의 내역은 조회할 수 없음
        if (!application.getDriver().getId().equals(driverId)
                || !isHistory(application)) {
            throw new CustomException(
                    ErrorCode.NOT_FOUND,
                    "지난 배차 내역을 찾을 수 없습니다.");
        }

        return toHistoryResponse(application);
    }

    // 지난 내역에 포함할 신청 상태 구분
    private boolean isHistory(DispatchApplication application) {

        DispatchApplicationStatus status = application.getStatus();

        if (status == DispatchApplicationStatus.WITHDRAWN
                || status == DispatchApplicationStatus.REJECTED
                || status == DispatchApplicationStatus.NOT_SELECTED) {
            return true;
        }

        // 선택된 기사의 완료·취소 배송도 지난 내역에 포함
        ShipmentStatus shipmentStatus = application.getShipment().getStatus();

        return status == DispatchApplicationStatus.SELECTED
                && (shipmentStatus == ShipmentStatus.COMPLETED
                        || shipmentStatus == ShipmentStatus.CANCELED);
    }

    // 신청 기록을 응답 DTO로 변환
    private DriverDispatchHistoryResponseDTO toHistoryResponse(
            DispatchApplication application) {

        Shipment shipment = application.getShipment();

        // 실제 배송 완료 상태로 변경된 시각 조회
        LocalDateTime completedAt = null;

        if (application.getStatus() == DispatchApplicationStatus.SELECTED
                && shipment.getStatus() == ShipmentStatus.COMPLETED) {

            completedAt = statusHistoryRepository
                    .findFirstByShipment_IdAndToStatusOrderByChangedAtDesc(
                            shipment.getId(), ShipmentStatus.COMPLETED)
                    .map(history -> history.getChangedAt())
                    .orElse(null);
        }

        return DriverDispatchHistoryResponseDTO.builder()
                .applicationId(application.getId())
                .shipmentId(shipment.getId())
                .applicationStatus(application.getStatus().name())
                .shipmentStatus(shipment.getStatus().name())
                .cargoName(shipment.getCargoName())
                .cargoWeightKg(shipment.getCargoWeightKg())
                .vehicleTypeId(shipment.getVehicleType().getId())
                .fare(shipment.getFare())
                .originAddress(shipment.getOriginAddress())
                .destAddress(shipment.getDestAddress())
                .pickupAt(shipment.getPickupAt())
                .appliedAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .completedAt(completedAt)
                .build();
    }

}
