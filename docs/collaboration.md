# 협업 규칙

## 역할 분담 (도메인별 세로 분담: 테이블 → API → 화면)

| 담당 | 도메인 | 테이블 | 핵심 로직 (발표) |
|---|---|---|---|
| 팀원 1 (팀장) | 공통 · 인증 · 운영 | member, term, term_agreement, notice, faq, inquiry | 공통 골격 · JWT, 통계, HTTPS 배포 |
| 팀원 2 | 기사 · 차량 · 배차 · 정산 | driver, vehicle, vehicle_type, driver_schedule, dispatch, settlement | 배차 후보 추천, 월 정산 |
| 팀원 3 | 배송 접수 · 운임 · 결제 | shipment, cargo_type, payment | 카카오 연동 · 운임, 결제 승인 · 환불 |
| 팀원 4 | 배송 진행 · 실시간 · 리뷰 | shipment_status_history, shipment_image, location_log, notification, review | 상태 규칙, SSE 알림 · 위치 |

## 공통 책임

| 역할 | 담당 | 하는 일 |
|---|---|---|
| 팀장 · 일정 | 1 | 주간 회의, 이슈 보드, 완료 기준 점검 |
| 배포 · 인프라 | 1 | AWS · Nginx · HTTPS, 키 관리 |
| Git · ERD · 시드 | 2 | develop → main 병합, docs/erd.md, data.sql |
| API 문서 | 3 | Swagger, docs/api.md, Postman 컬렉션 |
| UI 통일 | 4 | 색 · 상태 배지 · components/common, 9주차 화면 점검 |

## 도메인 간 약속 (먼저 빈 구현으로 develop에 올린다)

| 언제까지 | 제공 → 받는 사람 | 약속 |
|---|---|---|
| 2주차 말 | 1 → 전원 | 공통 골격: BaseEntity, 예외 · 에러 응답, PageRequestDTO/PageResponseDTO, CORS, 임시 보안(전체 허용) |
| 3주차 초 | 1 → 전원 | 로그인 사용자 꺼내는 방법(예: @AuthenticationPrincipal), 역할별 URL 규칙 |
| 3주차 중 | 3 → 2·4 | Shipment 엔티티 · ShipmentStatus · Repository |
| 3주차 중 | 2 → 3 | VehicleType 엔티티 · 초기 데이터 |
| 3주차 중 | 2 → 1 | `DriverService.register(member, licenseType, careerYears)` · `getStatus(memberId)` (팀원 2 부재 중 팀원 1이 작성) |
| 4주차 | 4 → 전원 | `NotificationService.send(receiver, type, message, shipment)` (처음엔 로그만) |
| 4주차 | 4 → 2·3 | `StatusHistoryService.record(shipment, from, to, changedBy, memo)` |
| 5주차 | 2 → 4 | `DispatchService.getActiveDriver(shipmentId)` → 배정 기사의 **회원 ID**(`member.id`, `driver.id` 아님), 배정이 없으면 `null` (상태 변경 · 이력 조회가 로그인 회원 ID와 비교한다). 실구현 PR에서 `DispatchServiceMock`을 지운다 (#60) |
| 5주차 | 3 → 전원 | 개발용 결제 건너뛰기 API (local 프로필만) |
| 8주차 | 4 → 2 | `ReviewService.getAverageRating(driverId)` — `driverId`는 `driver.id` (Review가 Driver를 가리킨다. 위 `getActiveDriver`의 회원 ID와 다름) |
| 7주차 (T) | 3 → 1 | 대시보드 집계(읽기 전용): `ShipmentService.countByStatus()` · `PaymentService.getPaidStat(from, to)` · `getDailyPaidStats(fromDate, toDate)` · `getPaidStatsByVehicleType(from, to)` (팀원 1이 작성, 팀원 3 리뷰) |
| 보안 점검 후속 | 1 → 전원 | `MemberService.findById(memberId)` → `Optional<Member>` (회원 엔티티가 필요할 때 `MemberRepository` 대신, 예: 배송 신청 고객 연결) |
| 2단계 예정 | 4 → 1 | 대시보드 오늘 완료 건수(상태 이력의 COMPLETED 기준) · 전체 평균 평점 — 이름은 그때 정한다 |
| 2단계 예정 | 2 → 1 | 대시보드 기사 순위용 기사별 완료 건수 — 이름은 그때 정한다 |

다른 도메인은 위 Service 메서드로만 호출한다. 다른 사람의 Repository를 직접 쓰지 않는다. 약속이 늦어지면 데일리 스크럼에서 바로 공유한다.

### 로그인 사용자 꺼내기 (3주차 초 약속)

```java
@GetMapping("/my")
@PreAuthorize("hasRole('CUSTOMER')")  // 로그인만 필요하면 isAuthenticated(), 관리자는 hasRole('ADMIN')
public PageResponseDTO<ShipmentDTO> my(@AuthenticationPrincipal LoginMember loginMember, @Valid PageRequestDTO pageRequestDTO) {
	return shipmentService.getMyList(loginMember.getId(), pageRequestDTO);  // getId() · getEmail() · getRole()
}
```
- `LoginMember`는 액세스 토큰 안의 값이라 DB를 다시 조회하지 않는다. 회원 정보가 더 필요하면 `MemberService.getMember(id)`
- 2026-11-09 권한 전환부터 공개 API가 아니면 토큰 없이 부를 수 없다(401) → `loginMember`는 null이 아니다
- `/api/admin/**` · `/api/driver/**`는 URL 규칙이 역할까지 검사한다. 그 밖의 주소에서 역할이 정해진 API(예: 고객만 배송 신청)는 `@PreAuthorize`를 꼭 붙인다
- 로그인 없이 써야 하는 API를 새로 만들면 SecurityConfig 공개 목록에 넣어 달라고 팀원 1에게 요청한다 (api.md 권한 "전체")

## Git

- `main`: 발표 · 배포용. 직접 push 금지. 주차 완료 기준을 통과한 develop만 팀원 2가 병합
- `develop`: 통합 브랜치(기본 브랜치). PR로만 병합. 빌드가 깨지면 최우선으로 고친다
- 작업 브랜치: `feature/{도메인}-{기능}` (예: feature/dispatch-candidate), `fix/...`
- 하루 1번 이상 `git pull origin develop`으로 충돌을 작게 유지
- 커밋: `feat: 배차 후보 조회 API (#12)` · `fix:` · `refactor:` · `docs:` · `test:` · `chore:`

## PR · 리뷰

- PR 하나에 기능 하나, 300줄 안팎. 큰 기능은 "엔티티 · API" → "화면"으로 나눈다
- 짝 리뷰: 1 ↔ 3, 2 ↔ 4. 공통 파일을 건드리면 그 파일 관리자도 승인
- 리뷰는 24시간 안에. Squash merge 후 브랜치 삭제
- AI로 만든 부분은 PR에 표시. 본인이 설명할 수 있는 코드만 올린다

## 공통 파일 (PR + 관리자 승인)

| 파일 | 관리자 | 규칙 |
|---|---|---|
| SecurityConfig, JWTCheckFilter | 1 | URL 권한 추가는 요청 또는 PR 승인 |
| application.yml, build.gradle | 1 | 의존성 추가 시 PR에 이유 (버전은 수업과 동일) |
| ErrorCode, GlobalExceptionHandler | 1 | ErrorCode는 도메인별 구역, 자기 구역에만 추가 |
| data.sql, docs/erd.md | 2 | 테이블 · 초기 데이터 변경은 팀원 2가 반영 |
| docs/api.md, Swagger 설정 | 3 | API 변경 PR에 문서 수정 포함 |
| package.json, router/root.jsx, layouts/ | 1 | 라이브러리 · 라우트 추가는 PR로 |
| components/common/, Tailwind 설정 | 4 | 공통 컴포넌트 추가 · 수정은 팀원 4 승인 |
| CLAUDE.md | 전원 | 주간 회의 합의 후 PR |

## DB

- 각자 로컬 DB (docker-compose.yml). DB 자체는 공유하지 않는다
- 엔티티 변경 PR에는 docs/erd.md (필요하면 data.sql) 수정 포함, 병합 후 메신저 공지
- 공지를 받으면 pull 후 서버 재실행. 컬럼 삭제 시 `docker compose down -v && docker compose up -d`로 초기화

## 회의 · 기록

- 데일리 스크럼 10분 (메신저 가능): 어제 한 일 · 오늘 할 일 · 막힌 것
- 주간 회의 30~60분: 완료 기준 시연 → 다음 주 계획 → ERD · API 변경 확정 → 리스크
- 이슈 보드: GitHub Projects (To do / In progress / Review / Done), roadmap.md 체크 항목을 이슈로
- 결정은 docs/decisions.md, 트러블슈팅은 docs/troubleshooting/{이름}.md

## 키 · 비밀 정보
- 카카오 REST/JS 키, 토스 테스트 키, JWT secret은 Git에 올리지 않고 팀 비공개 메모로 공유
- 실수로 커밋했다면 지워도 기록에 남으므로 즉시 재발급
