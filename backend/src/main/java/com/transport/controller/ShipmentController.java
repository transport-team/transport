package com.transport.controller;

import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.ShipmentService; // 추가
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.ShipmentListResponseDTO;
import com.transport.dto.ShipmentDetailResponseDTO;
import com.transport.domain.enums.ShipmentStatus;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

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

    private final ShipmentService shipmentService; // 의존성 추가

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

    @Operation(summary = "내 배송 목록", description = "로그인한 고객의 배송을 최신 신청 순으로 조회합니다.")
    @PreAuthorize("hasRole('CUSTOMER')")
    @GetMapping("/my")
    public ResponseEntity<PageResponseDTO<ShipmentListResponseDTO>> getMyShipments(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid PageRequestDTO pageRequestDTO,
            @RequestParam(required = false) ShipmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(shipmentService.getMyShipments(
                loginMember.getId(), pageRequestDTO, status, from, to));
    }

    @Operation(summary = "배송 상세", description = "본인·배정 기사·관리자만 배송 상세를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ShipmentDetailResponseDTO> getShipment(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginMember loginMember) {
        return ResponseEntity.ok(shipmentService.getShipmentDetail(
                id, loginMember.getId(), loginMember.getRole()));
    }
}