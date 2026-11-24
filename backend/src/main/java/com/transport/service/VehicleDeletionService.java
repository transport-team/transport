package com.transport.service;

import java.util.List;
import com.transport.dto.VehicleDeletionResponseDTO;

public interface VehicleDeletionService {
    Long apply(Long memberId, Long vehicleId);
    List<VehicleDeletionResponseDTO> getMyRequests(Long memberId);
    void withdraw(Long memberId, Long requestId);
    List<VehicleDeletionResponseDTO> getAllRequests();
    void approve(Long requestId);
    void reject(Long requestId, String reason);
}
