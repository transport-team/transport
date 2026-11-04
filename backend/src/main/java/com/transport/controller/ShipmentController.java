package com.transport.controller;

import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import com.transport.service.FareService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shipment API", description = "배송 접수 및 운임 산정 API")
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final FareService fareService;

    @Operation(summary = "운임 미리보기 계산", description = "출발지, 목적지, 무게 등을 바탕으로 예상 운임을 계산합니다.")
    @PostMapping("/calculate-fare")
    public ResponseEntity<FareCalculateResponseDTO> calculateFare(
            @Valid @RequestBody FareCalculateRequestDTO requestDTO) {
        
        FareCalculateResponseDTO response = fareService.calculateFare(requestDTO);
        return ResponseEntity.ok(response);
    }
}