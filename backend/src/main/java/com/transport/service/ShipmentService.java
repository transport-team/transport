package com.transport.service;

import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;

import java.util.Map;

public interface ShipmentService {
    ShipmentResponseDTO createShipment(Long memberId, ShipmentRequestDTO requestDTO);

    // 관리자 대시보드: 지금 상태별 배송 건수 (모든 상태가 들어 있고, 배송이 없는 상태는 0)
    Map<ShipmentStatus, Long> countByStatus();
}
