package com.transport.service;

import java.util.List;
import com.transport.dto.AdminDriverVehicleResponseDTO;

public interface AdminDriverVehicleService {
    List<AdminDriverVehicleResponseDTO> getDriverVehicles(Long driverId);
}
