package com.transport.service;

import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;

public interface ShipmentService {
    
    /**
     * 배송 접수 및 결제 대기 정보 생성
     */
    ShipmentResponseDTO createShipment(ShipmentRequestDTO requestDTO);
}