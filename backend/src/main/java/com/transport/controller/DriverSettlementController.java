package com.transport.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.transport.dto.DriverMonthlySettlementResponseDTO;
import com.transport.dto.DriverSettlementItemResponseDTO;
import com.transport.dto.DriverSettlementRangeResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.DriverSettlementService;

import lombok.RequiredArgsConstructor;

// 기사: 월별 배송 완료 건수 및 운임 합계 조회 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/settlements")
@PreAuthorize("hasRole('DRIVER')")
public class DriverSettlementController {

    private final DriverSettlementService driverSettlementService;

    // 로그인한 기사의 월별 정산 요약 조회
    @GetMapping("/monthly")
    public ResponseEntity<DriverMonthlySettlementResponseDTO> getMonthlySummary(
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestParam String month) {

        return ResponseEntity.ok(
                driverSettlementService.getMonthlySummary(
                        loginMember.getId(), month));
    }

    // 로그인한 기사의 월별 운임 상세 내역 조회
    @GetMapping("/monthly/items")
    public ResponseEntity<List<DriverSettlementItemResponseDTO>> getMonthlyItems(
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestParam String month) {

        return ResponseEntity.ok(
                driverSettlementService.getMonthlyItems(
                        loginMember.getId(), month));
    }

    // 로그인한 기사의 기간별 정산 요약
    @GetMapping("/range")
    public ResponseEntity<DriverSettlementRangeResponseDTO> getRangeSummary(
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                driverSettlementService.getRangeSummary(
                        loginMember.getId(), from, to));
    }

    // 로그인한 기사의 기간별 운임 상세 내역
    @GetMapping("/range/items")
    public ResponseEntity<List<DriverSettlementItemResponseDTO>> getRangeItems(
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        return ResponseEntity.ok(
                driverSettlementService.getRangeItems(
                        loginMember.getId(), from, to));
    }

}
