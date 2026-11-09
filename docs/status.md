# 배송 상태 · 전이 규칙

4명이 함께 쓰는 가장 중요한 규칙. 바꾸려면 주간 회의에서 합의 후 이 문서부터 수정한다.

## 상태 정의

| 상태 (enum) | 한글 | 의미 | 누가 바꾸나 |
|---|---|---|---|
| PENDING_PAYMENT | 결제대기 | 신청서 저장, 결제 대기 (기사 운송 요청 목록에 안 보임) | 고객 (신청) |
| REQUESTED | 접수 | 결제 완료, 배차 대기 (기사 지원을 받는 중) | 시스템 (결제 승인) |
| ASSIGNED | 배차완료 | 기사·차량 배정 | 고객 (지원한 기사 선택) |
| PICKED_UP | 상차완료 | 화물 적재 (상차 사진) | 배정된 기사 |
| IN_TRANSIT | 운송중 | 이동 중 — 이때만 위치 공유 | 배정된 기사 |
| ARRIVED | 도착 | 도착, 인계 전 | 배정된 기사 |
| COMPLETED | 배송완료 | 인계 완료 (인수증 필수), 이후 변경 불가, 리뷰 가능 | 배정된 기사 |
| CANCELED | 취소 | 결제대기·접수·배차완료에서 취소 (결제된 건은 자동 환불) | 고객·관리자·시스템 |

## 흐름

```
PENDING_PAYMENT → REQUESTED → ASSIGNED → PICKED_UP → IN_TRANSIT → ARRIVED → COMPLETED
       │              │  ↑         │
       │              │  └─배차 해제┘
       ↓              ↓            ↓
    CANCELED       CANCELED     CANCELED
 (고객 취소·30분)  (환불)       (관리자, 환불)
```

## 전이 규칙

| 현재 → 다음 | 누가 | 조건 | 함께 처리 | 담당 |
|---|---|---|---|---|
| (신규) → 결제대기 | 고객 | 신청서 검증 통과 | 운임 계산·저장, payment READY 생성 | 3 |
| 결제대기 → 접수 | 시스템 | 토스 승인 성공 + 금액 일치 | payment PAID, 관리자 알림 | 3 |
| 결제대기 → 취소 | 고객·시스템 | 고객 취소 또는 30분 경과(스케줄러) | payment EXPIRED | 3 |
| 접수 → 배차완료 | 고객 | 지원한 기사 중 선택 (그 지원이 대기 중). 기사가 지원할 때: ACTIVE, 근무 일정 안, 차량 AVAILABLE, 적재량 ≥ 무게, 당일 배차 수 < 한도 | dispatch 생성, 다른 지원은 마감, 선택된 기사 알림 | 2 |
| 배차완료 → 접수 | 관리자 | 배차 해제 | dispatch active = false | 2 |
| 배차완료 → 상차완료 | 배정 기사 | 상차 사진 1장 이상 | 고객 알림 | 4 |
| 상차완료 → 운송중 | 배정 기사 | — | 위치 공유 시작, 고객 알림 | 4 |
| 운송중 → 도착 | 배정 기사 | — | 위치 공유 중지, 고객 알림 | 4 |
| 도착 → 배송완료 | 배정 기사 | 인수증 사진 1장 이상 | 고객 알림(리뷰 요청) | 4 |
| 접수 → 취소 | 고객·관리자 | 사유 입력 | 결제 취소(환불), 알림 | 3 |
| 배차완료 → 취소 | 관리자 | 사유 입력 | dispatch 비활성화, 환불, 고객·기사 알림 | 3 |

모든 전이에서 `shipment_status_history`에 (이전 상태, 다음 상태, 변경자, 사유, 시각) 저장. 이력 저장 메서드는 팀원 4가 만들고 모두 호출한다.

배차는 기사가 운송 요청에 지원하고 고객이 지원한 기사 중에서 고른다 (2026-11-06 팀 결정, decisions.md). 관리자는 배차 해제 · 취소를 맡는다.

## 구현

```java
public enum ShipmentStatus {
    PENDING_PAYMENT, REQUESTED, ASSIGNED, PICKED_UP, IN_TRANSIT, ARRIVED, COMPLETED, CANCELED;

    // 상태별로 갈 수 있는 다음 상태 (COMPLETED, CANCELED는 끝 상태)
    private static final Map<ShipmentStatus, Set<ShipmentStatus>> NEXT = Map.of(
        PENDING_PAYMENT, Set.of(REQUESTED, CANCELED),
        REQUESTED,       Set.of(ASSIGNED, CANCELED),
        ASSIGNED,        Set.of(PICKED_UP, REQUESTED, CANCELED),
        PICKED_UP,       Set.of(IN_TRANSIT),
        IN_TRANSIT,      Set.of(ARRIVED),
        ARRIVED,         Set.of(COMPLETED)
    );

    public boolean canChangeTo(ShipmentStatus next) {
        return NEXT.getOrDefault(this, Set.of()).contains(next);
    }
}

// Shipment 엔티티 — "누가"는 각 Service에서, "갈 수 있나"는 여기서 검사
public void changeStatus(ShipmentStatus next) {
    if (!this.status.canChangeTo(next)) {
        throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
    }
    this.status = next;
}
```
