package com.transport.service;

import java.util.List;

import com.transport.dto.DispatchApplicantResponseDTO;
import com.transport.dto.DriverShipmentListResponseDTO;

public interface DispatchApplicationService {

    // 기사: 원하는 운송 요청에 차량을 선택해서 지원
    Long apply(Long memberId, Long shipmentId, Long vehicleId);

    // 고객이 자신의 배송에 지원한 기사 목록 조회
    List<DispatchApplicantResponseDTO> getApplicants(Long memberId, Long shipmentId);

    // 고객이 지원 기사 한 명을 선택하여 배차 확정
    Long selectApplicant(Long memberId, Long shipmentId, Long applicationId);

    // 고객이 지원한 기사를 개별적으로 거절
    void rejectApplicant(Long memberId, Long shipmentId, Long applicationId);

    // 기사: 배차 대기 중인 운송 요청 목록 조회
    List<DriverShipmentListResponseDTO> getAvailableShipments(Long memberId);
}
