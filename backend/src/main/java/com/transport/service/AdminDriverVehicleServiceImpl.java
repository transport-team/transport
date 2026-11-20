package com.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.dto.AdminDriverVehicleResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;
import com.transport.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDriverVehicleServiceImpl implements AdminDriverVehicleService {

    private final DriverRepository driverRepository;
    private final VehicleRepository vehicleRepository;

    @Override
    public List<AdminDriverVehicleResponseDTO> getDriverVehicles(Long driverId) {
        if (!driverRepository.existsById(driverId)) {
            throw new CustomException(ErrorCode.NOT_FOUND, "기사 정보를 찾을 수 없습니다.");
        }
        return vehicleRepository.findByDriver_IdOrderByCreatedAtDescIdDesc(driverId).stream()
                .map(vehicle -> new AdminDriverVehicleResponseDTO(
                        vehicle.getId(),
                        vehicle.getPlateNo(),
                        vehicle.getVehicleType().getName(),
                        vehicle.getStatus(),
                        vehicle.getCreatedAt()))
                .toList();
    }
}
