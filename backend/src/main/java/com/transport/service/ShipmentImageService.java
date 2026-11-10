package com.transport.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.transport.domain.enums.ShipmentImageType;
import com.transport.dto.ShipmentImageResponseDTO;

// 배송 사진 서비스 (팀원 4 — 5-1, docs/collaboration.md)
public interface ShipmentImageService {

	/**
	 * 배송 사진 업로드 (배정 기사만)
	 * @param shipmentId 배송 ID
	 * @param files 사진 파일들 (1장 이상)
	 * @param type 사진 종류 (PICKUP / RECEIPT / ETC)
	 * @param memberId 로그인 기사 회원 ID
	 * @return 저장된 사진 목록
	 */
	List<ShipmentImageResponseDTO> upload(Long shipmentId, List<MultipartFile> files, ShipmentImageType type, Long memberId);
}