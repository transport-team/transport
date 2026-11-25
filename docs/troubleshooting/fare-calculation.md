# 트러블슈팅: 서버 기준 운임 계산 로직 분리 및 단위 테스트 검증

- **작성자 / 담당**: 이시은 (팀원 3 — 배송 접수 · 운임 · 결제)
- **관련 도메인**: 운임 계산(`FareServiceImpl`, `FareCalculator`), 배송 접수(`ShipmentServiceImpl`)

## 1. 증상 및 배경
1. 배송 접수 및 운임 산정 시 클라이언트가 전달한 거리·할증·금액 값을 신뢰하지 않고, 서버에서 거리 계산 및 상차일시(`pickupAt`)·중량 기준 할증을 일관되게 적용해야 함.
2. 기본운임, 거리 초과분 올림(`Math.ceil`), 화물 추가요금, 심야·주말·최대 적재 중량 80% 초과 할증률 합산 및 100원 미만 버림 규칙이 실제 코드에서 의도대로 동작하는지 검증 테스트가 필요함.

## 2. 원인 분석
1. 운임 산정 책임이 분리되어 있지 않으면 배송 접수(`createShipment`)와 견적 계산(`calculateFare`) 간 금액 불일치나 할증 누락이 발생할 수 있음.
2. `FareCalculator`의 할증 조건(`22시~06시` 야간, `SATURDAY`/`SUNDAY` 주말, `weightKg * 100 > maxLoadKg * 80` 중량 초과)과 `total / 100 * 100` 절사 로직, 그리고 `FareServiceImpl`의 차종 추천(`recommendedVehicleTypeId` / `inquiryRequired`) 조합을 자동화된 테스트로 고정해 두어야 회귀를 막을 수 있음.

## 3. 해결 방법
1. `FareServiceImpl.calculateFare`에서 차종·화물유형 조회, `DistanceService.findRoute` 기반 경로 조회, `FareCalculator.calculate` 및 `distanceFare` 호출, 적재 중량 기준 최소 차종 추천(`recommendedVehicleTypeId`, `inquiryRequired`)을 수행하도록 구성함.
2. `FareServiceTest` 단위 테스트를 작성해 평일 주간 기본 조건, 거리 올림 + 화물 추가요금 반영, 심야·주말·중량 초과 할증 합산 및 100원 단위 절사 시나리오를 검증함.
3. Windows 환경에서 `./gradlew` 대신 `gradlew.bat test --tests com.transport.service.FareServiceTest`로 테스트를 실행해 통과를 확인함.

## 4. 검증 결과
- `gradlew.bat test --tests com.transport.service.FareServiceTest` 실행 결과 `BUILD SUCCESSFUL`로 전체 테스트 시나리오 통과.