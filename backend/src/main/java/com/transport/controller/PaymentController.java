package com.transport.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.PaymentApproveRequestDTO;
import com.transport.dto.PaymentConfirmResponseDTO;
import com.transport.service.PaymentService;
import com.transport.security.LoginMember;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Payment API", description = "결제 승인 및 검증 API")
@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 승인 및 검증", description = "결제 요청 금액을 검증하고 배송 상태를 접수 완료로 변경합니다.")
    @PostMapping("/confirm")
    public ResponseEntity<PaymentConfirmResponseDTO> confirmPayment(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody PaymentApproveRequestDTO requestDTO) {
        return ResponseEntity.ok(paymentService.verifyAndApprovePayment(requestDTO, loginMember.getId()));
    }
}