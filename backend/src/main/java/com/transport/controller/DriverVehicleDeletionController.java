package com.transport.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.transport.dto.VehicleDeletionResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.VehicleDeletionService;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/vehicles/deletion-requests")
@PreAuthorize("hasRole('DRIVER')")
public class DriverVehicleDeletionController {
    private final VehicleDeletionService service;

    @GetMapping
    public List<VehicleDeletionResponseDTO> myRequests(@AuthenticationPrincipal LoginMember member) {
        return service.getMyRequests(member.getId());
    }

    @PostMapping("/{vehicleId}")
    public Long apply(@AuthenticationPrincipal LoginMember member, @PathVariable Long vehicleId) {
        return service.apply(member.getId(), vehicleId);
    }

    @DeleteMapping("/{requestId}")
    public void withdraw(@AuthenticationPrincipal LoginMember member, @PathVariable Long requestId) {
        service.withdraw(member.getId(), requestId);
    }
}
