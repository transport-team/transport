package com.transport.service;

import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.ShipmentListResponseDTO;
import com.transport.dto.ShipmentDetailResponseDTO;
import com.transport.domain.enums.MemberRole;
import java.time.LocalDate;

import java.util.List;
import java.util.Map;

public interface ShipmentService {
    ShipmentResponseDTO createShipment(Long memberId, ShipmentRequestDTO requestDTO);

    PageResponseDTO<ShipmentListResponseDTO> getMyShipments(
            Long memberId, PageRequestDTO pageRequestDTO, ShipmentStatus status,
            LocalDate from, LocalDate to);

    ShipmentDetailResponseDTO getShipmentDetail(Long shipmentId, Long memberId, MemberRole role);

    // 관리자 대시보드: 지금 상태별 배송 건수 (모든 상태가 들어 있고, 배송이 없는 상태는 0)
    Map<ShipmentStatus, Long> countByStatus();

    // 배차 서비스에서 사용할 배송 단건 조회
    Shipment getShipment(Long shipmentId);

    // 배송 단건을 비관적 쓰기 잠금으로 조회 — 호출한 트랜잭션이 끝날 때까지 다른 변경을 막는다
    // (팀원 2 관리자 배차 취소가 저장소 대신 부름, 2026-11-25). 없으면 SHIPMENT_NOT_FOUND
    Shipment getShipmentForUpdate(Long shipmentId);

    // 기사: 배차 대기 중인 운송 요청 목록 조회
    List<Shipment> getRequestedShipments();
}
