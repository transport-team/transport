package com.transport.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.transport.domain.enums.ShipmentImageType;
import com.transport.dto.ShipmentImageResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.ShipmentImageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

// 배송 사진 API (팀원 4 — 5-1) — POST /api/shipments/{id}/images
@Tag(name = "Shipment Image API", description = "배송 사진 업로드 (배정 기사만)")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipments")
public class ShipmentImageController {

	private final ShipmentImageService shipmentImageService;

	/**
	 * 배송 사진 업로드 (배정 기사만)
	 * @param id 배송 ID
	 * @param files 사진 파일들 (multipart, 1장 이상)
	 * @param type 사진 종류 — PICKUP(상차) / RECEIPT(인수증) / ETC
	 */
	@Operation(summary = "배송 사진 업로드", description = "배정 기사가 상차·인수증 사진을 올린다. multipart/form-data: files(여러 장), type")
	@PostMapping(value = "/{id}/images", consumes = "multipart/form-data")
	@PreAuthorize("hasRole('DRIVER')")
	public ResponseEntity<List<ShipmentImageResponseDTO>> uploadImages(
			@PathVariable Long id,
			@RequestPart("files") List<MultipartFile> files,
			@RequestPart("type") String type,
			@AuthenticationPrincipal LoginMember loginMember
	) {
		ShipmentImageType imageType = ShipmentImageType.valueOf(type.toUpperCase());
		return ResponseEntity.ok(
				shipmentImageService.upload(id, files, imageType, loginMember.getId()));
	}
}