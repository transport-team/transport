# 운임 계산 규칙 (담당: 팀원 3)

예시 값이며 실제 시세가 아니다. 값은 vehicle_type · cargo_type 테이블에 있고 관리자 화면에서 바꿀 수 있다.

## 차종 (vehicle_type)

| 코드 | 이름 | 최대 적재 | 기본요금 (10km 포함) | 초과 1km당 | 무게 기준 자동 추천 |
|---|---|---|---|---|---|
| SMALL | 소형 (다마스·라보) | 500kg | 30,000 | 1,000 | 500kg 이하 |
| TON1 | 1톤 | 1,000kg | 50,000 | 1,500 | 500 초과 ~ 1,000kg |
| TON2_5 | 2.5톤 | 2,500kg | 90,000 | 2,200 | 1,000 초과 ~ 2,500kg |
| TON5 | 5톤 | 5,000kg | 150,000 | 3,000 | 2,500 초과 ~ 5,000kg (초과 시 문의 안내) |

## 화물 종류 (cargo_type)

| 이름 | 추가요금 | 취급 안내 |
|---|---|---|
| 일반 | 0 | — |
| 파손주의 | 5,000 | 완충재 포장 확인, 적재 시 위에 다른 짐 금지 |
| 가구 · 가전 | 10,000 | 상하차 시 고객과 함께 확인 |

## 계산식

운임 = 기본요금 + (거리를 1km 단위로 올림 − 10km) × km당 요금 + 화물 추가요금
→ 할증률을 **합산해 한 번에** 적용 → 100원 미만 버림

할증: 야간(상차 22:00 ~ 06:00) +20% · 주말(토·일) +10% · 고중량(차종 최대 적재량의 80% 초과) +10%

## 검증 예시 (FareCalculator 단위 테스트에 그대로 사용)

| 예시 | 조건 | 계산 | 운임 |
|---|---|---|---|
| ① 기본 | 1톤, 38.4km, 600kg, 일반, 2026-12-03(목) 09:00 | 50,000 + (39 − 10) × 1,500 | 93,500 |
| ② 할증 | 1톤, 38.4km, 900kg, 일반, 2026-12-05(토) 22:30 | 93,500 × 1.4 (야간 20 + 주말 10 + 고중량 10) | 130,900 |
| ③ 화물 | 1톤, 38.4km, 600kg, 파손주의, 2026-12-03(목) 09:00 | 93,500 + 5,000 | 98,500 |

```java
public int calculate(VehicleType type, CargoType cargo,
                     double distanceKm, int weightKg, LocalDateTime pickupAt) {
    int overKm = Math.max(0, (int) Math.ceil(distanceKm) - type.getBaseDistanceKm());
    int fare = type.getBaseFare() + overKm * type.getPerKmFare() + cargo.getExtraFee();

    int rate = 0;                                            // 할증률 합산(%)
    int hour = pickupAt.getHour();
    if (hour >= 22 || hour < 6) rate += type.getNightRate();
    DayOfWeek day = pickupAt.getDayOfWeek();
    if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) rate += type.getWeekendRate();
    if (weightKg * 100 > type.getMaxLoadKg() * 80) rate += type.getHeavyRate();   // 80% 초과

    int total = fare + fare * rate / 100;
    return total / 100 * 100;                               // 100원 미만 버림
}
```

## 거리 구하기
- 주소 → 좌표: 카카오 로컬 API `GET https://dapi.kakao.com/v2/local/search/address.json?query=주소`, 헤더 `Authorization: KakaoAK {REST_API_KEY}` (응답의 x = 경도, y = 위도)
- 도로 거리 · 소요시간: 카카오모빌리티 자동차 길찾기 API (요청 형식 · 무료 쿼터는 카카오모빌리티 디벨로퍼스 문서 확인)
- 길찾기 실패 시: 하버사인 직선거리 × 1.3 (예시 보정 계수)
- 같은 출발 · 도착 구간 결과는 재사용해 쿼터 절약. 외부 API는 서버에서만 호출
