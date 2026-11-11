package com.transport.domain.enums;

import java.util.Map;
import java.util.Set;

// 배송 상태 (docs/status.md)
public enum ShipmentStatus {
        PENDING_PAYMENT,   // 결제대기
        REQUESTED,         // 접수 (결제 완료, 배차 대기)
        ASSIGNED,          // 배차완료
        PICKED_UP,         // 상차완료
        IN_TRANSIT,        // 운송중
        ARRIVED,           // 도착
        COMPLETED,         // 배송완료
        CANCELED;          // 취소

        // 상태별로 변경 가능한 다음 상태
        private static final Map<ShipmentStatus, Set<ShipmentStatus>> NEXT = Map.of(
                PENDING_PAYMENT, Set.of(REQUESTED, CANCELED),
                REQUESTED, Set.of(ASSIGNED, CANCELED),
                ASSIGNED, Set.of(PICKED_UP, REQUESTED, CANCELED),
                PICKED_UP, Set.of(IN_TRANSIT),
                IN_TRANSIT, Set.of(ARRIVED),
                ARRIVED, Set.of(COMPLETED)
        );

        // 현재 상태에서 다음 상태로 변경할 수 있는지 확인
        public boolean canChangeTo(ShipmentStatus next) {
                return NEXT.getOrDefault(this, Set.of()).contains(next);
        }
}
