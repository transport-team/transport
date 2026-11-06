package com.transport.domain.enums;

// 기사 배차 지원 상태
public enum DispatchApplicationStatus {
    PENDING,      // 물주 선택 대기
    SELECTED,     // 물주가 선택하여 배차 확정
    REJECTED,     // 물주가 직접 거절
    NOT_SELECTED  // 다른 기사가 선택되어 자동 마감
}
