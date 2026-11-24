package com.transport.dto;

import java.util.List;

// 확정된 금액을 변경하지 않고 현재 실적과 마감 당시 스냅샷의 차이만 읽는다.
public record AdminSettlementDifferenceDTO(
        String month, Long driverId, String driverName, String note,
        List<Line> lines) {
    public record Line(Long shipmentId, Integer fixedFare, Integer currentFare, String differenceType) { }
}
