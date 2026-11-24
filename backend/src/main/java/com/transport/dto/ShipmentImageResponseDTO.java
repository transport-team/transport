package com.transport.dto;

import com.transport.domain.ShipmentImage;
import com.transport.domain.enums.ShipmentImageType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 배송 사진 응답 (팀원 4 — 5-1)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentImageResponseDTO {

	private Long id;
	private ShipmentImageType type;
	private String fileName; // UUID_원본명
	private String url;      // GET /api/files/{fileName} (<img>에서 바로 사용)

	public static ShipmentImageResponseDTO from(ShipmentImage image) {
		return ShipmentImageResponseDTO.builder()
				.id(image.getId())
				.type(image.getType())
				.fileName(image.getFileName())
				.url("/api/files/" + image.getFileName())
				.build();
	}
}