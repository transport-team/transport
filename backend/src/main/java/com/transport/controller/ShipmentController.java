package com.transport.controller;

import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.FareService;
import com.transport.service.ShipmentService; // 추가

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Shipment API", description = "배송 접수 및 운임 산정 API")
@RestController
@RequestMapping("/api/shipments")
@RequiredArgsConstructor
public class ShipmentController {

    private final FareService fareService;
    private final ShipmentService shipmentService; // 의존성 추가

    @Operation(summary = "운임 미리보기 계산", description = "출발지, 목적지, 무게 등을 바탕으로 예상 운임을 계산합니다.")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/calculate-fare")
    public ResponseEntity<FareCalculateResponseDTO> calculateFare(
            @Valid @RequestBody FareCalculateRequestDTO requestDTO) {
        
        FareCalculateResponseDTO response = fareService.calculateFare(requestDTO);
        return ResponseEntity.ok(response);
    }

    // 신규 추가: 배송 접수 API
    @Operation(summary = "배송 접수 (결제 대기)", description = "배송을 접수하고 결제 대기 상태의 주문을 생성합니다.")
    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("")
    public ResponseEntity<ShipmentResponseDTO> createShipment(
    		@AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody ShipmentRequestDTO requestDTO) {
        
        // 서버에서 운임 재계산 및 배송/결제 엔티티 생성
        ShipmentResponseDTO response = shipmentService.createShipment(loginMember.getId(), requestDTO);
        
        // 생성 성공 시 201 Created 반환
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}