package com.transport.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.VehicleTypeRequestDTO;
import com.transport.dto.VehicleTypeResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.service.VehicleTypeService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/vehicle-types")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleTypeController {

    private final VehicleTypeService vehicleTypeService;

    @GetMapping
    public List<VehicleTypeResponseDTO> list() {
        return vehicleTypeService.getAllVehicleTypesForAdmin();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleTypeResponseDTO register(@Valid @RequestBody VehicleTypeRequestDTO request) {
        return vehicleTypeService.registerVehicleType(request);
    }

    @PutMapping("/{id}")
    public VehicleTypeResponseDTO update(@PathVariable Long id, @Valid @RequestBody VehicleTypeRequestDTO request) {
        return vehicleTypeService.updateVehicleType(id, request);
    }

    @PatchMapping("/{id}/active")
    public VehicleTypeResponseDTO changeActive(@PathVariable Long id, @RequestBody Map<String, Boolean> body) {
        if (!body.containsKey("active") || body.get("active") == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "활성화 여부를 선택해 주세요.");
        }
        return vehicleTypeService.changeVehicleTypeActive(id, body.get("active"));
    }
}
