package com.transport.service;

import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;

public interface ShipmentService {
    ShipmentResponseDTO createShipment(Long memberId, ShipmentRequestDTO requestDTO);
}