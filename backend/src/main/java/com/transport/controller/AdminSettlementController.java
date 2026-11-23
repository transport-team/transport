package com.transport.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import com.transport.security.LoginMember;
import com.transport.dto.AdminSettlementPaymentConfirmDTO;
import com.transport.dto.AdminSettlementDifferenceDTO;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.AdminSettlementMonthResponseDTO;
import com.transport.dto.AdminSettlementTrendDTO;
import java.util.List;
import com.transport.service.AdminSettlementService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/settlements")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSettlementController {

    private final AdminSettlementService settlementService;

    @GetMapping
    public AdminSettlementMonthResponseDTO monthly(@RequestParam String month) {
        return settlementService.getMonthlySettlements(month);
    }

    @GetMapping("/trend")
    public List<AdminSettlementTrendDTO> trend(@RequestParam String endMonth) {
        return settlementService.getMonthlyTrend(endMonth);
    }

    // 월 마감은 스케줄러만 수행한다. 관리자 수동 마감 엔드포인트는 제공하지 않는다.
    @GetMapping("/differences")
    public AdminSettlementDifferenceDTO differences(@RequestParam String month,
            @RequestParam Long driverId) {
        return settlementService.getDifferences(month, driverId);
    }

    @PostMapping("/{id}/paid")
    public ResponseEntity<AdminSettlementMonthResponseDTO.Item> markPaid(
            @PathVariable Long id,
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody AdminSettlementPaymentConfirmDTO request) {
        return ResponseEntity.ok(settlementService.markPaid(id, loginMember.getEmail(), request.transferReference()));
    }
}
