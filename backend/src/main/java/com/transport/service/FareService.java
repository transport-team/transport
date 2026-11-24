package com.transport.service;

import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;

public interface FareService {
    FareCalculateResponseDTO calculateFare(FareCalculateRequestDTO requestDTO);
}