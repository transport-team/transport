package com.transport.dto;

import java.time.LocalDateTime;

// 기사 한 명의 기간 안 운송완료 배송 한 건 (StatusHistoryService → 기사 실적 운임 내역, 팀원 4 → 2 약속)
public record DriverCompletedShipmentDTO(Long shipmentId, String cargoName, int fare, LocalDateTime completedAt) {
}
