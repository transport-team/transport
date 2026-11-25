# 트러블슈팅: 관리자 배송 검색 기사 조건 누락 및 상세 배차·결제 정보 미노출

- **작성자 / 담당**: 이시은 (팀원 3 — 배송 접수 · 운임 · 결제)
- **관련 도메인**: 관리자 배송 관리, 배차(`Dispatch`), 결제(`Payment`), 배송 상세(`ShipmentDetailResponseDTO`)

## 1. 증상 및 배경
1. 관리자 배송 목록 검색(`searchAdminShipments`)에서 배차된 기사명이나 기사 ID로 검색해도 결과가 조회되지 않음.
2. 목록에서 `배차완료` 상태인 배송 건의 상세 화면에 진입해도 배차 기사 정보(`ASSIGNED DRIVER`)가 `"현재 배차된 기사가 없습니다."`로 표시되고 결제 정보(`PAYMENT`) 섹션도 보이지 않음.

## 2. 원인 분석
1. **검색 쿼리 조인 및 조건 누락**: `ShipmentRepository.searchAdminShipments`가 `Dispatch`, `Driver`, `Member`를 조인하지 않고 배송/화물/주소/고객 필드만 검색하고 있었음.
2. **상세 응답 DTO 및 서비스 매핑 부재**: `ShipmentDetailResponseDTO`에 배차 기사 상세 정보와 결제 정보가 담기지 않았고, `ShipmentServiceImpl.getShipmentDetail`에서도 활성 배차(`active = true`) 및 결제 정보를 조회해 내려주지 않았음.
3. **재시작/캐시 반영 타이밍**: 백엔드 쿼리·DTO·서비스 수정 직후 이전 빌드 상태로 요청이 처리되면서 구 쿼리가 실행되는 현상이 함께 나타남.

## 3. 해결 방법
1. `ShipmentRepository.searchAdminShipments`에 `left join Dispatch d on d.shipment = s and d.active = true` 및 `driver`·`member` 좌측 조인을 추가하고(배차 없는 배송 누락 방지를 위해 `left join` 사용), `keyword` 조건에 활성 배차 기사명(`lower(dm.name)`)과 기사 ID(`str(dr.id)`) 검색을 포함함.
2. `ShipmentDetailResponseDTO`에 `DriverInfoDTO`, `PaymentInfoDTO` 및 기존 호출부 호환성을 유지하는 오버로딩 `from` 메서드를 추가하고, `ShipmentServiceImpl.getShipmentDetail`에서 활성 배차와 `Payment` 정보를 조회해 매핑함.
3. `ShipmentManagePage.jsx` 검색창 placeholder를 실제 검색 조건에 맞게 안내하고, `AdminShipmentDetailPage.jsx`에 `ASSIGNED DRIVER` 및 `PAYMENT` 섹션을 추가한 뒤 백엔드 서버를 재시작해 반영함.

## 4. 검증 결과
- 관리자 배송 통합 검색 실행 시 `left join dispatch ... and d1_0.active=true` 및 기사명·기사 ID 조건이 포함된 SQL이 정상 실행됨.
- 배차 완료 건 상세 조회 시 `dispatch`, `driver`, `member`, `vehicle`, `vehicle_type`, `payment` 조회가 이루어지고 화면에 배차 기사 및 결제 정보가 표시됨.