package com.transport.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import com.transport.dto.VehicleDeletionResponseDTO;
import com.transport.dto.VehicleRegistrationRejectDTO;
import com.transport.service.VehicleDeletionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/vehicle-deletion-requests")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleDeletionController {
    private final VehicleDeletionService service;

    @GetMapping
    public List<VehicleDeletionResponseDTO> all() { return service.getAllRequests(); }

    @PatchMapping("/{requestId}/approve")
    public void approve(@PathVariable Long requestId) { service.approve(requestId); }

    @PatchMapping("/{requestId}/reject")
    public void reject(@PathVariable Long requestId, @Valid @RequestBody VehicleRegistrationRejectDTO requestDTO) {
        service.reject(requestId, requestDTO.getRejectionReason());
    }
}
