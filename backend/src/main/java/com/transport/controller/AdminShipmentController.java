// AdminShipmentController.java (com.transport.controller)
package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.AdminShipmentResponseDTO;
import com.transport.dto.AdminShipmentSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.service.AdminShipmentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Admin Shipment API", description = "관리자 배송 통합 검색 API (ADM-02)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/shipments")
@PreAuthorize("hasRole('ADMIN')")
public class AdminShipmentController {

	private final AdminShipmentService adminShipmentService;

	@Operation(summary = "관리자 배송 통합 검색", description = "상태, 신청일 기간, 키워드(배송번호/화물/주소/고객/수령인)로 전체 배송을 검색합니다.")
	@GetMapping
	public PageResponseDTO<AdminShipmentResponseDTO> list(@Valid AdminShipmentSearchDTO searchDTO) {
		return adminShipmentService.searchShipments(searchDTO);
	}
}