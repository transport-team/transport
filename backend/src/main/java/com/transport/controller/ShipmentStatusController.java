package com.transport.controller;

import com.transport.dto.StatusChangeRequestDTO;
import com.transport.security.LoginMember;
import com.transport.service.StatusService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 배송 상태 변경 API (팀원 4) — PATCH /api/shipments/{id}/status
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipments")
public class ShipmentStatusController {

	private final StatusService statusService;

	/**
	 * 배송 상태 변경 (배정 기사만)
	 * @param id 배송 ID
	 * @param request { nextStatus, memo }
	 */
	@PatchMapping("/{id}/status")
	@PreAuthorize("hasRole('DRIVER')")
	public ResponseEntity<Void> changeStatus(
			@PathVariable Long id,
			@Valid @RequestBody StatusChangeRequestDTO request,
			@AuthenticationPrincipal LoginMember loginMember
	) {
		statusService.changeStatus(id, request.getNextStatus(), request.getMemo(), loginMember.getId());
		return ResponseEntity.noContent().build();
	}
}