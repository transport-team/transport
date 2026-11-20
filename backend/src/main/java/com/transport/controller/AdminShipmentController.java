// AdminShipmentController.java (com.transport.controller)
package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.transport.security.LoginMember;
import com.transport.dto.AdminDispatchCancelRequestDTO;
import com.transport.service.AdminDispatchCancellationService;
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
    private final AdminDispatchCancellationService cancellationService;

	@Operation(summary = "관리자 배송 통합 검색", description = "상태, 신청일 기간, 키워드(배송번호/화물/주소/고객/수령인)로 전체 배송을 검색합니다.")
	@GetMapping
	public PageResponseDTO<AdminShipmentResponseDTO> list(@Valid AdminShipmentSearchDTO searchDTO) {
		return adminShipmentService.searchShipments(searchDTO);
	}

    // 관리자만 배차 취소: 결제 및 배송 주문은 유지, 배차는 비활성화
    @PostMapping("/{id}/dispatch/cancel")
    public ResponseEntity<Void> cancelDispatch(
            @PathVariable("id") Long shipmentId,
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody AdminDispatchCancelRequestDTO body) {
        cancellationService.cancelDispatch(shipmentId, loginMember.getId(), body.reason());
        return ResponseEntity.noContent().build();
    }
}