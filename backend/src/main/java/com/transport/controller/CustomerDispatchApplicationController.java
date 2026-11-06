package com.transport.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.DispatchApplicantResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.DispatchApplicationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/customer/shipments")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerDispatchApplicationController {

    private final DispatchApplicationService dispatchApplicationService;

    // 고객이 자신의 배송에 지원한 기사 목록 조회
    @GetMapping("/{shipmentId}/applications")
    public ResponseEntity<List<DispatchApplicantResponseDTO>> getApplicants(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable("shipmentId") Long shipmentId) {

        List<DispatchApplicantResponseDTO> applicants =
                dispatchApplicationService.getApplicants(loginMember.getId(), shipmentId);

        return ResponseEntity.ok(applicants);
    }

    // 고객이 지원 기사 한 명을 선택하여 배차 확정
    @PostMapping("/{shipmentId}/applications/{applicationId}/select")
    public ResponseEntity<Long> selectApplicant(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable("shipmentId") Long shipmentId,
            @PathVariable("applicationId") Long applicationId) {

        Long dispatchId = dispatchApplicationService.selectApplicant(
                loginMember.getId(), shipmentId, applicationId);

        return ResponseEntity.ok(dispatchId);
    }

}
