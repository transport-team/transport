package com.transport.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.DriverAssignedDispatchResponseDTO;
import com.transport.dto.DriverDispatchHistoryResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.DriverMyDispatchService;

import lombok.RequiredArgsConstructor;

// 기사: 본인 확정 배차 관리 API
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/dispatches")
@PreAuthorize("hasRole('DRIVER')")
public class DriverMyDispatchController {

    private final DriverMyDispatchService driverMyDispatchService;

    // 본인의 진행 중인 확정 배차 목록
    @GetMapping
    public ResponseEntity<List<DriverAssignedDispatchResponseDTO>> getMyDispatches(
            @AuthenticationPrincipal LoginMember loginMember) {

        return ResponseEntity.ok(
                driverMyDispatchService.getMyDispatches(loginMember.getId()));
    }

    // 본인의 확정 배차 상세
    @GetMapping("/{shipmentId}")
    public ResponseEntity<DriverAssignedDispatchResponseDTO> getMyDispatch(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable("shipmentId") Long shipmentId) {

        return ResponseEntity.ok(
                driverMyDispatchService.getMyDispatch(
                        loginMember.getId(), shipmentId));
    }

    // 기사: 본인의 지난 신청 및 배송 내역
    @GetMapping("/history")
    public ResponseEntity<List<DriverDispatchHistoryResponseDTO>> getMyHistory(
            @AuthenticationPrincipal LoginMember loginMember) {

        return ResponseEntity.ok(
                driverMyDispatchService.getMyHistory(loginMember.getId()));
    }

    // 기사: 지난 내역 상세 조회
    @GetMapping("/history/{applicationId}")
    public ResponseEntity<DriverDispatchHistoryResponseDTO> getMyHistoryDetail(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable("applicationId") Long applicationId) {

        return ResponseEntity.ok(
                driverMyDispatchService.getMyHistoryDetail(
                        loginMember.getId(), applicationId));
    }
}