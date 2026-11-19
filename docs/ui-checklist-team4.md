# UI 점검표 — 팀원 4 (배송 진행·실시간·리뷰)

> 작성일: 2026-11-12  
> 대상 화면: 기사 처리 모바일, 고객 배송 상세(타임라인·지도·리뷰), 헤더 알림 종  
> 기준: docs/requirements.md(COM-04, CUS-05, CUS-07, DRV-03~06) + Tailwind 색 팔레트(index.css)

---

## 1. 팀 공통 색 팔레트 (index.css @theme)

| 용도 | 색상 변수 | HEX | 용례 |
|------|-----------|-----|------|
| **주색 (Primary)** | `--color-primary` | `#059669` | 주요 버튼, 활성 상태, 링크 |
| 주색 호버 | `--color-primary-hover` | `#047857` | 버튼 호버 |
| 주색 라이트 | `--color-primary-light` | `#d1fae5` | 배지 배경, 칩 |
| 주색 텍스트 | `--color-primary-text` | `#065f46` | 버튼 텍스트 |
| **위험 (Danger)** | `--color-danger` | `#dc2626` | 삭제, 취소, 에러 알림 |
| 위험 호버 | `--color-danger-hover` | `#b91c1c` | |
| 위험 라이트 | `--color-danger-light` | `#fee2e2` | 에러 배너 배경 |
| 위험 텍스트 | `--color-danger-text` | `#991b1b` | 에러 텍스트 |
| **성공 (Success)** | `--color-success` | `#16a34a` | 완료, 성공 토스트 |
| 성공 라이트 | `--color-success-light` | `#dcfce7` | 완료 배지 |
| 성공 텍스트 | `--color-success-text` | `#166534` | |
| **경고 (Warning)** | `--color-warning` | `#d97706` | 대기, 주의 |
| 경고 라이트 | `--color-warning-light` | `#fef3c7` | 대기 배지 |
| 경고 텍스트 | `--color-warning-text` | `#92400e` | |
| **정보 (Info)** | `--color-info` | `#2563eb` | 진행 중, 안내 |
| 정보 라이트 | `--color-info-light` | `#dbeafe` | 진행 배지 |
| 정보 텍스트 | `--color-info-text` | `#1e40af` | |
| **중립/회색** | `--color-gray-neutral` | `#6b7280` | 비활성, 보조 텍스트 |
| 회색 라이트 | `--color-gray-light` | `#f3f4f6` | 카드 배경 |
| 회색 텍스트 | `--color-gray-text` | `#374151` | 본문 텍스트 |

---

## 2. 공통 컴포넌트 사용 확인표

| 컴포넌트 | 파일 | 적용 화면 | 비고 |
|----------|------|-----------|------|
| `Button` | `components/common/Button.jsx` | 기사 처리, 리뷰 폼, 알림 드롭다운 | variant: primary/danger/outline/ghost |
| `Input` | `components/common/Input.jsx` | 리뷰 후기 textarea | |
| `Modal` | `components/common/Modal.jsx` | (향후) 사진 확대, 확인 다이얼로그 | |
| `Table` | `components/common/Table.jsx` | (관리자) 리뷰 관리표 | |
| `Pagination` | `components/common/Pagination.jsx` | 알림 목록, 리뷰 목록 | |
| `Loading` | `components/common/Loading.jsx` | 기사 처리 초기 로딩, 지도 로딩, 리뷰 제출 중 | |
| `StatusBadge` | `components/common/StatusBadge.jsx` | **전 화면** — 기사 처리 상단, 타임라인 각 행, 리뷰 섹션 | **필수 사용** |
| `NotificationBell` | `components/common/NotificationBell.jsx` | 헤더 (팀원 1 소유 Layout에 포함) |

---

## 3. 화면별 상세 점검표

### 3-1. 기사 배송 처리 화면 (`/driver/dispatches/:id`) — DRV-03, DRV-04, DRV-05, DRV-06

| # | 항목 | 요구사항 | 확인 기준 | 상태 |
|---|------|----------|-----------|------|
| 1 | **로딩 상태** | DRV-03 초기 진입 시 | `Loading` 스피너 표시, 데이터 도착 전 UI 비활성화 | [ ] |
| 2 | **빈 목록/에러** | 배송 없음, 네트워크 에러 | 빈 화면 안내("배정된 배송이 없습니다"), 에러 토스트 + 재시도 버튼 | [ ] |
| 3 | **화물·수령인 정보** | DRV-03 | cargoName, weight, origin/dest address, receiverName/Phone 표시 | [ ] |
| 4 | **전화 걸기** | DRV-03 | `tel:` 링크로 receiverPhone 연결 | [ ] |
| 5 | **상태 배지** | DRV-04 현재 상태 표시 | `StatusBadge` 사용, 색상 팔레트 준수 (ASSIGNED=primary, PICKED_UP=primary, IN_TRANSIT=info, ARRIVED=warning, COMPLETED=success) | [ ] |
| 6 | **"다음 단계" 버튼 1개만** | DRV-04 | 현재 상태에 맞는 단일 버튼만 표시 (ASSIGNED→PICKED_UP, PICKED_UP→IN_TRANSIT, IN_TRANSIT→ARRIVED, ARRIVED→COMPLETED) | [ ] |
| 7 | **상태 변경 호출** | DRV-04 | `PATCH /api/shipments/{id}/status` 호출, 로딩 중 버튼 disabled, 성공 시 배지 갱신 | [ ] |
| 8 | **에러 처리** | DRV-04 | PHOTO_REQUIRED → "상차 사진을 먼저 올려주세요", NOT_ASSIGNED_DRIVER → "배정된 기사만 가능", INVALID_STATUS_CHANGE → "잘못된 상태 변경" 등 사람 말로 안내 | [ ] |
| 9 | **사진 업로드 영역** | DRV-05 | `<input type=file accept=image/* capture=environment>` PICKUP/RECEIPT 구분, 미리보기 썸네일, 업로드 실패 시 인라인 에러 | [ ] |
| 10 | **위치 공유 표시** | DRV-06 | IN_TRANSIT일 때만 "공유 중 · n초 전 전송" 배지, 상태 변경/화면 이탈 시 자동 중지 | [ ] |
| 11 | **모바일 반응형** | DRV-03~06 | 375px 기준 터치 타겟 ≥44px, 세로 스크롤 자연스러움, 가로 스크롤 없음 | [ ] |
| 12 | **금액·날짜 형식** | 공통 | 금액: `toLocaleString()` 콤마, 날짜: `YYYY-MM-DD HH:mm` (KST) | [ ] |

---

### 3-2. 고객 배송 상세 — 타임라인 영역 (CUS-05)

| # | 항목 | 요구사항 | 확인 기준 | 상태 |
|---|------|----------|-----------|------|
| 1 | **로딩/빈/에러** | CUS-05 | `Loading` → 데이터 → `Timeline` 렌더, 이력 0건 시 "상태 변경 이력이 없습니다", 에러 시 토스트 + 재시도 | [ ] |
| 2 | **데이터 소스** | CUS-05 | `GET /api/shipments/{id}/history` 호출, 본인·배정기사·관리자만 접근(403 처리) | [ ] |
| 3 | **표시 항목** | CUS-05 | 상태(한글 라벨) · 변경 시각 · 변경자 이름 · 메모 | [ ] |
| 4 | **상태 배지** | CUS-05 | `StatusBadge` 사용 (size: sm 권장), 색상 팔레트 준수 | [ ] |
| 5 | **시각 정렬** | CUS-05 | 최신순(desc) 또는 오름차순(asc) — requirements 확인 후 통일 | [ ] |
| 6 | **모바일 대응** | CUS-05 | 카드형 타임라인, 가로 스크롤 없음, 터치 영역 충분 | [ ] |
| 7 | **날짜 형식** | 공통 | `YYYY-MM-DD HH:mm` | [ ] |

---

### 3-3. 고객 배송 상세 — 지도 영역 (DRV-06 연동, CUS-05)

| # | 항목 | 요구사항 | 확인 기준 | 상태 |
|---|------|----------|-----------|------|
| 1 | **로딩/에러** | CUS-05 | 카카오맵 로딩 중 `Loading`, 로드 실패 시 안내 문구 + 재시도 | [ ] |
| 2 | **초기 위치** | DRV-06 | `GET /api/shipments/{id}/locations/latest`로 첫 마커 표시 | [ ] |
| 3 | **실시간 이동** | DRV-06 | SSE `location` 이벤트 수신 시 마커 부드럽게 이동 (팬/줌 안 함) | [ ] |
| 4 | **마지막 업데이트 표시** | DRV-06 | "마지막 위치 n초 전" 텍스트 실시간 갱신 | [ ] |
| 5 | **완료 시 숨김** | 개인정보 | COMPLETED/CANCELED 상태면 지도·마커 미표시 (또는 마지막 위치만 고정 안 함) | [ ] |
| 6 | **환경변수 키** | 보안 | `import.meta.env.VITE_KAKAO_MAP_KEY` 사용, 코드에 하드코딩 없음 | [ ] |
| 7 | **좌표 순서** | 정확도 | 카카오맵은 **경도(x), 위도(y)** 순서 — `lng, lat` 확인 | [ ] |
| 8 | **모바일** | CUS-05 | 전체 폭, 높이 300~400px, 터치 줌/팬 가능 | [ ] |

---

### 3-4. 고객 배송 상세 — 리뷰 영역 (CUS-07)

| # | 항목 | 요구사항 | 확인 기준 | 상태 |
|---|------|----------|-----------|------|
| 1 | **조건부 렌더** | CUS-07 | COMPLETED + 내 배송 + 리뷰 없음 → `ReviewSection` 작성 폼 표시 / 리뷰 있음 → 읽기 전용 / 그 외 → 미표시 | [ ] |
| 2 | **작성 폼** | CUS-07 | 별점 1~5 (반별 불가), 후기 500자 카운터, `Input`(textarea) 사용 | [ ] |
| 3 | **제출 검증** | CUS-07 | 별점 필수, 후기 500자 초과 방지, 제출 중 `Loading` + 중복 클릭 방지 | [ ] |
| 4 | **에러 안내** | CUS-07 | REVIEW_EXISTS → "이미 리뷰를 작성하셨습니다", REVIEW_NOT_ALLOWED → "배송 완료 후 작성 가능" | [ ] |
| 5 | **표시** | CUS-07 | 평균 평점(소수 1자리) + 리뷰 카드(별점+후기+작성일) | [ ] |
| 6 | **모바일** | CUS-07 | 별점 터치 영역 충분, 텍스트에어리어 높이 자동 확장 | [ ] |

---

### 3-5. 헤더 알림 종 (COM-04)

| # | 항목 | 요구사항 | 확인 기준 | 상태 |
|---|------|----------|-----------|------|
| 1 | **SSE 연결** | COM-04 | `event-source-polyfill`로 Authorization 헤더 포함 연결, `GET /api/notifications/subscribe` | [ ] |
| 2 | **자동 재연결** | COM-04 | 연결 끊김 시 지수 백오프(1s, 2s, 4s… 최대 30s) 재시도 | [ ] |
| 3 | **안 읽은 개수 배지** | COM-04 | 헤더 종 아이콘 위 빨간 뱃지, 실시간 갱신 (`GET /api/notifications` unreadCount) | [ ] |
| 4 | **드롭다운 목록** | COM-04 | 최신 10건 미리보기, 타입별 아이콘(배차/상태변경/문의답변/리뷰요청), 시간 상대표시(방금 전, n분 전) | [ ] |
| 5 | **클릭 시 읽음 처리** | COM-04 | 알림 클릭 → `PATCH /api/notifications/{id}/read` → 해당 배송 상세 페이지 이동 | [ ] |
| 6 | **새 알림 토스트** | COM-04 | SSE 수신 즉시 우상단 토스트(3초 자동 닫힘), 타입별 색상(danger/info/success) | [ ] |
| 7 | **모바일** | COM-04 | 드롭다운 전체 폭, 터치 친화적 | [ ] |

---

## 4. 공통 UX 체크리스트 (전 화면)

| 항목 | 상세 | 상태 |
|------|------|------|
| **검색/필터 조건 URL 쿼리스트링 유지** | (해당 화면: 알림 목록, 리뷰 목록) 새로고침/뒤로가기 시 조건 복원 | [ ] |
| **에러 코드 → 사람 말 변환** | 모든 API 에러: `ErrorCode`별 메시지 매핑표 적용 (ex. PHOTO_REQUIRED → "사진을 먼저 등록해주세요") | [ ] |
| **로딩 스켈레톤** | 데이터 로딩 중 카드/리스트 자리잡기용 스켈레톤(회색 펄스) | [ ] |
| **빈 상태 일러스트 + 안내** | 데이터 0건 시 아이콘 + "~가 없습니다" + 주요 액션 버튼(있다면) | [ ] |
| **토스트 통일** | 성공(초록), 에러(빨강), 경고(노랑), 정보(파랑) — `useToast` 훅 공통화 | [ ] |
| **접근성** | 버튼/링크 포커스 아웃라인, aria-label(아이콘 버튼), 색상 대비(WCAG AA) | [ ] |

---

## 5. 검증 방법

1. **Storybook/컴포넌트 단위** — `StatusBadge`, `Button` 등 각 상태별 렌더 스냅샷
2. **브라우저 실기** — Chrome DevTools Device Toolbar (iPhone 14 / Galaxy S23)
3. **네트워크 느림 시뮬** — Fast 3G / Offline 에서 로딩·에러·빈 상태 확인
4. **SSE 연결 테스트** — 네트워크 탭에서 EventSource 연결 유지·재연결·heartbeat(30s) 확인
5. **카카오맵 키** — `.env.local` 미설정 시 콘솔 에러·대체 UI 확인

---

## 6. 팀 공유 사항 (수정 필요 시)

| 항목 | 담당 | 요청 내용 |
|------|------|-----------|
| `router/root.jsx` | 팀원 1 | `/driver/dispatches/:id` 라우트 추가 확인 |
| `layouts/MainLayout.jsx` (헤더) | 팀원 1 | `NotificationBell` import 위치 확정 |
| `package.json` | 팀원 1 | `event-source-polyfill` 의존성 이미 있음 (로컬 npm install만) |
| `SecurityConfig` | 팀원 1 | `/api/files/**` permitAll 유지, `/api/notifications/subscribe` 인증 필요 |

---

> **완료 기준**: 위 모든 항목 `[x]` 체크 + `npm run build` 성공 + 모바일 실기 통과