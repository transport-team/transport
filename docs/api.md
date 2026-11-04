# API 명세

- 관리: 팀원 3 (Swagger UI와 함께 최신 유지). API를 바꾸는 PR에는 이 문서 수정을 포함한다.

## 공통 규칙
- 기본 경로 `/api` · 관리자 `/api/admin/**` · 기사 `/api/driver/**`
- 인증 `Authorization: Bearer {accessToken}` — 401 = 로그인 필요(없음·만료), 403 = 권한 없음
  - 401 `UNAUTHORIZED` 토큰 없음 · `TOKEN_EXPIRED` 만료 → `/api/auth/refresh`로 재발급 후 다시 요청 · `INVALID_TOKEN` 다시 로그인
  - 2026-11-09 권한 전환부터: 공개 API 외에는 모두 로그인이 필요하다. `/api/admin/**`는 ADMIN, `/api/driver/**`는 DRIVER만 (SecurityConfig URL 규칙)
  - 공개 API(아래 표의 권한 "전체"): `/api/auth/**` · GET `/api/health` · `/api/terms` · `/api/notices`(`/{id}`) · `/api/faqs` · `/api/vehicle-types` · `/api/cargo-types` · `/api/files/{fileName}` · Swagger. 새로 공개할 API는 팀원 1에게 요청
  - 그 밖의 역할(예: 배송 신청은 CUSTOMER)은 각 컨트롤러의 `@PreAuthorize`로 검사한다. 공개 API에도 잘못되거나 만료된 토큰을 보내면 401
- 목록: 수업의 `PageRequestDTO`(page는 1부터 · size 기본 10 · 최대 100) / `PageResponseDTO`(dtoList, pageNumList, prev · next, prevPage · nextPage, totalCount, totalPage, current). 검색 파라미터 이름 `status, from, to, keyword`
- 에러 응답: `{ "code": "INVALID_STATUS_CHANGE", "message": "배차완료 상태에서만 상차 처리할 수 있습니다." }`

## 에러 코드 (ErrorCode — 도메인별 구역에 각자 추가)

사용: `throw new CustomException(ErrorCode.NOT_FOUND)` → HTTP 상태 · 기본 메시지는 ErrorCode에 둔다. 상황별 문구는 `new CustomException(ErrorCode.X, "문구")`. `@Valid` 실패는 `INVALID_INPUT` + DTO에 적은 message

| 구역 | 코드 예시 |
|---|---|
| 공통·인증 (1) | INVALID_INPUT, UNAUTHORIZED, FORBIDDEN, NOT_FOUND, METHOD_NOT_ALLOWED, FILE_TOO_LARGE, INTERNAL_SERVER_ERROR, TERMS_REQUIRED, DUPLICATE_EMAIL, LOGIN_FAILED, TOKEN_EXPIRED, INVALID_TOKEN, ACCOUNT_DISABLED, DRIVER_NOT_APPROVED, WRONG_PASSWORD |
| 배차 (2) | ALREADY_DISPATCHED, NO_AVAILABLE_VEHICLE, OUT_OF_SCHEDULE, OVER_DAILY_LIMIT, SETTLEMENT_EXISTS |
| 배송·결제 (3) | ADDRESS_NOT_FOUND, ROUTE_NOT_FOUND, OVER_MAX_WEIGHT, PAYMENT_AMOUNT_MISMATCH, PAYMENT_FAILED, CANNOT_CANCEL |
| 진행 (4) | INVALID_STATUS_CHANGE, NOT_ASSIGNED_DRIVER, PHOTO_REQUIRED, NOT_IN_TRANSIT, REVIEW_EXISTS |

## 팀원 1 — 회원 · 인증 · 운영

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| GET | /api/health | 서버 동작 확인 → `{"status":"ok"}` | 전체 |
| POST | /api/auth/signup | 회원가입 (필수 약관 동의, 기사는 PENDING) | 전체 |
| POST | /api/auth/login | 로그인 → 토큰 · 역할 | 전체 |
| POST | /api/auth/refresh | 토큰 재발급 | 전체 |
| GET · PUT | /api/members/me | 내 정보 조회 · 수정(연락처) | 로그인 |
| PATCH | /api/members/me/password | 비밀번호 변경 (현재 비밀번호 확인) | 로그인 |
| GET | /api/terms | 가입 화면 약관 목록 | 전체 |
| GET | /api/notices, /api/notices/{id} | 공지 목록 · 상세 | 전체 |
| GET | /api/faqs | FAQ (category) | 전체 |
| POST · GET | /api/inquiries, /api/inquiries/my | 문의 등록 · 내 문의 | 로그인 |
| GET | /api/admin/dashboard | KPI | ADMIN |
| GET | /api/admin/stats/daily | 일별 건수 · 매출 (from, to) | ADMIN |
| GET · PATCH | /api/admin/members, /{id}/enabled | 회원 검색 · 비활성화 | ADMIN |
| POST · PUT · DELETE | /api/admin/notices · /faqs · /terms (/{id}) | 공지 · FAQ · 약관 관리 | ADMIN |
| PATCH | /api/admin/inquiries/{id}/answer | 문의 답변 (알림) | ADMIN |

### 회원가입 예시

```jsonc
// GET /api/terms → 가입 화면 약관 (required = 필수 동의)
[{ "id": 1, "title": "서비스 이용약관", "content": "…", "required": true, "version": "v1.0" }]

// POST /api/auth/signup — 고객
{ "email": "kim@test.com", "password": "password1!", "name": "김고객", "phone": "010-1234-5678",
  "role": "CUSTOMER", "agreedTermIds": [1, 2] }
// POST /api/auth/signup — 기사: licenseType · careerYears 필수 → 승인 대기(PENDING)로 가입, 관리자 승인 후 로그인
{ "email": "lee@test.com", "password": "password1!", "name": "이기사", "phone": "010-2222-3333",
  "role": "DRIVER", "agreedTermIds": [1, 2], "licenseType": "1종 보통", "careerYears": 5 }
// 201
{ "memberId": 4 }
// 409 DUPLICATE_EMAIL · 400 TERMS_REQUIRED(필수 약관 빠짐) · 400 INVALID_INPUT(형식 오류 · 없는 약관 · ADMIN 가입 · 기사 정보 빠짐)
```
- 비밀번호 8~64자, 연락처 `010-0000-0000` 형식, 면허 종류 20자 이하, 경력 0~60년

### 로그인 · 재발급 예시

```jsonc
// POST /api/auth/login (시드 계정: admin · customer · driver @transport.test / 1234)
{ "email": "customer@transport.test", "password": "1234" }
// 200 — role로 역할별 첫 화면 이동
{ "accessToken": "eyJ…", "refreshToken": "eyJ…", "memberId": 2, "email": "customer@transport.test", "name": "테스트고객", "role": "CUSTOMER" }
// 401 LOGIN_FAILED(없는 이메일 · 틀린 비밀번호 같은 응답) · 403 ACCOUNT_DISABLED(비활성 계정 · 중지된 기사) · 403 DRIVER_NOT_APPROVED(승인 대기 기사)

// POST /api/auth/refresh — 액세스 토큰이 TOKEN_EXPIRED일 때. 응답은 로그인과 같은 모양 (두 토큰 모두 새로 발급)
{ "refreshToken": "eyJ…" }
// 401 TOKEN_EXPIRED · INVALID_TOKEN → 로그인 화면으로

// GET /api/members/me (Authorization: Bearer {accessToken})
{ "id": 2, "email": "customer@transport.test", "name": "테스트고객", "phone": "010-0000-0002", "role": "CUSTOMER" }
```
- 토큰 유효 시간: 액세스 30분 · 리프레시 1일 (`application.yml`의 `app.jwt.*`)

### 내 정보 수정 예시

```jsonc
// PUT /api/members/me — 연락처만 바꿀 수 있다 (이름 · 이메일 · 역할은 그대로)
{ "phone": "010-9999-8888" }
// 200 바뀐 내 정보
{ "id": 2, "email": "customer@transport.test", "name": "테스트고객", "phone": "010-9999-8888", "role": "CUSTOMER" }
// 400 INVALID_INPUT(연락처 형식)

// PATCH /api/members/me/password
{ "currentPassword": "1234", "newPassword": "newPassword1!" }
// 204 (본문 없음) · 400 WRONG_PASSWORD(현재 비밀번호 틀림) · 400 INVALID_INPUT(새 비밀번호 8~64자 · 지금과 같은 비밀번호)
```
- 비밀번호를 바꿔도 이미 받은 토큰은 만료될 때까지 쓸 수 있다 (로그아웃시키지 않음)

## 팀원 2 — 기사 · 차량 · 배차 · 정산

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| GET | /api/vehicle-types | 차종 목록 | 전체 |
| GET · POST · PUT | /api/admin/vehicle-types(/{id}) | 차종 · 요금 관리 | ADMIN |
| GET · POST · PUT · DELETE | /api/admin/vehicles(/{id}) | 차량 관리 (DELETE = 비활성화) | ADMIN |
| GET · PATCH | /api/admin/drivers, /{id}/status | 기사 목록 · 승인/중지 | ADMIN |
| GET · POST · DELETE | /api/driver/schedules(/{id}) | 내 근무 일정 (month) | DRIVER |
| GET | /api/admin/dispatches/candidates | 배차 후보 (shipmentId) | ADMIN |
| POST | /api/admin/dispatches | 배차 { shipmentId, driverId, vehicleId } | ADMIN |
| PATCH | /api/admin/dispatches/{id}/release | 배차 해제 | ADMIN |
| GET | /api/driver/dispatches | 내 배차 (tab = today · upcoming · done) | DRIVER |
| GET | /api/driver/stats | 내 실적 · 평점 (month) | DRIVER |
| POST · GET | /api/admin/settlements | 월 정산 생성 · 목록 (month) | ADMIN |
| PATCH | /api/admin/settlements/{id}/pay | 지급 완료 | ADMIN |
| GET | /api/admin/stats/drivers | 기사별 실적 (month) | ADMIN |

### 기사 관리 예시 (관리자)

```jsonc
// GET /api/admin/drivers?status=PENDING&keyword=김&page=1 — status · keyword(이름 · 이메일 일부)는 빼도 된다, 최신 가입 순
{ "dtoList": [{ "id": 2, "memberId": 5, "name": "김기사", "email": "kim@test.com", "phone": "010-2222-3333",
                "licenseType": "1종 보통", "careerYears": 5, "status": "PENDING", "approvedAt": null, "createdAt": "2026-11-03T16:58:01" }],
  "totalCount": 1, "current": 1, "totalPage": 1, "pageNumList": [1], "prev": false, "next": false, … }

// PATCH /api/admin/drivers/{id}/status — 승인 ACTIVE · 중지 INACTIVE (PENDING으로는 되돌릴 수 없음)
{ "status": "ACTIVE" }
// 200 바뀐 기사 정보 (처음 승인할 때 approvedAt 기록) · 400 INVALID_INPUT · 404 NOT_FOUND · 403 FORBIDDEN(관리자 아님)
```

## 팀원 3 — 배송 접수 · 운임 · 결제

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| GET | /api/cargo-types | 화물 종류 목록 | 전체 |
| GET · POST · PUT | /api/admin/cargo-types(/{id}) | 화물 종류 관리 | ADMIN |
| POST | /api/fares/estimate | 예상 운임 → 좌표 · 거리 · 추천 차종 · 운임 내역 | CUSTOMER |
| POST | /api/shipments | 신청 → 결제대기 + 결제 준비(orderId · amount) | CUSTOMER |
| POST | /api/payments/confirm | 결제 승인 { paymentKey, orderId, amount } → 접수 | CUSTOMER |
| POST | /api/shipments/{id}/payment/retry | 재시도 (새 orderId) | CUSTOMER |
| POST | /api/dev/shipments/{id}/skip-payment | 개발용 결제 건너뛰기 (local 프로필만) | 로그인 |
| GET | /api/shipments/my | 내 배송 목록 (page, status, from, to) | CUSTOMER |
| GET | /api/shipments/{id} | 상세 (본인 · 배정 기사 · 관리자) | 로그인 |
| PATCH | /api/shipments/{id}/cancel | 취소 { reason } → 자동 환불 | CUSTOMER · ADMIN |
| GET | /api/admin/shipments | 전체 배송 검색 | ADMIN |
| GET | /api/admin/payments | 결제 · 환불 내역 | ADMIN |

## 팀원 4 — 배송 진행 · 실시간 · 리뷰

| 메서드 | URL | 설명 | 권한 |
|---|---|---|---|
| PATCH | /api/shipments/{id}/status | 상태 변경 { nextStatus, memo } — 배정 기사만 | DRIVER |
| GET | /api/shipments/{id}/history | 상태 이력 | 로그인 |
| POST | /api/shipments/{id}/images | 사진 업로드 (multipart: files, type) | DRIVER |
| GET | /api/files/{fileName} | 사진 보기 (썸네일 `s_`) — `<img>` 때문에 인증 없이, UUID 파일명 | 전체 |
| POST | /api/shipments/{id}/locations | 위치 전송 { lat, lng } — IN_TRANSIT만 | DRIVER |
| GET | /api/shipments/{id}/locations/latest | 마지막 위치 | 로그인 |
| GET | /api/notifications/subscribe | SSE 구독 (알림 · location 이벤트) | 로그인 |
| GET · PATCH | /api/notifications, /{id}/read | 알림 목록 · 읽음 | 로그인 |
| POST | /api/shipments/{id}/review | 리뷰 (배송완료 · 1회) | CUSTOMER |
| GET | /api/drivers/{id}/reviews | 기사 리뷰 · 평균 평점 | 로그인 |

## 결제 흐름

1. `POST /api/shipments` → 서버가 운임 계산, 배송 PENDING_PAYMENT + payment READY(orderId, amount) 저장
2. 프론트가 orderId · amount로 토스 결제창(테스트 키) 열기
3. 성공 → successUrl(`/customer/payments/success`) → `POST /api/payments/confirm`
4. 서버: 저장된 amount와 요청 amount 비교 → 토스 승인 API → PAID · REQUESTED · 관리자 알림
5. 실패 · 창 닫기 → failUrl에서 재시도 / 30분 지나면 스케줄러가 자동 취소

```jsonc
// POST /api/shipments
{
  "originAddress": "서울특별시 강남구 OO로 12", "originDetail": "1층 하역장",
  "destAddress": "경기도 수원시 영통구 OO로 34", "destDetail": "B동 창고",
  "cargoTypeId": 2, "cargoName": "유리 진열장", "cargoWeightKg": 600, "vehicleTypeId": 2,
  "pickupAt": "2026-12-03T09:00:00",
  "receiverName": "김수령", "receiverPhone": "010-0000-0000", "memo": "엘리베이터 없음"
}
// 201 — 1톤 · 38.4km · 평일 오전 · 파손주의 → 98,500원 (fare.md 예시 ③)
{ "id": 1024, "status": "PENDING_PAYMENT", "orderId": "SHP-1024-7f3a", "orderName": "1톤 운송 · 유리 진열장", "amount": 98500 }

// POST /api/payments/confirm
{ "paymentKey": "{토스가 넘겨준 값}", "orderId": "SHP-1024-7f3a", "amount": 98500 }
// 200
{ "shipmentId": 1024, "status": "REQUESTED", "paymentStatus": "PAID" }
```
