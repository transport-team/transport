// AdminPaymentController.java (com.transport.controller)
package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.AdminPaymentResponseDTO;
import com.transport.dto.AdminPaymentSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.AdminPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Payment API", description = "관리자 결제 내역 조회 API (ADM-03)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPaymentController {

	private final AdminPaymentService adminPaymentService;

	@Operation(summary = "관리자 결제 내역 검색", description = "결제 상태, 생성일 기간, 키워드(주문번호/배송번호/고객/화물명)로 결제 내역을 검색합니다.")
	@GetMapping
	public PageResponseDTO<AdminPaymentResponseDTO> list(@Valid AdminPaymentSearchDTO searchDTO) {
		return adminPaymentService.searchPayments(searchDTO);
	}
}