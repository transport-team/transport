package com.transport.dto;

import java.time.LocalDateTime;

import com.transport.domain.Member;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 상태 변경 이력 한 건 (타임라인 표시용, 팀원 4 — GET /api/shipments/{id}/history)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShipmentStatusHistoryResponseDTO {

	private Long id;
	private ShipmentStatus fromStatus;
	private ShipmentStatus toStatus;
	private String changedByName;
	private String memo;
	private LocalDateTime changedAt;

	public static ShipmentStatusHistoryResponseDTO from(ShipmentStatusHistory history) {
		Member changedBy = history.getChangedBy();
		return ShipmentStatusHistoryResponseDTO.builder()
				.id(history.getId())
				.fromStatus(history.getFromStatus())
				.toStatus(history.getToStatus())
				.changedByName(changedBy != null ? changedBy.getName() : null)
				.memo(history.getMemo())
				.changedAt(history.getChangedAt())
				.build();
	}
}