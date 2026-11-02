# 운송서비스 운용 사이트 (transport) — 4인 팀

화물 운송 회사가 배송 접수 → 결제 → 배차 → 운송 추적 → 정산까지 처리하는 웹 서비스.
역할: 고객(CUSTOMER), 기사(DRIVER), 관리자(ADMIN).

## 기술 스택
- Backend (backend/): Spring Boot 4.1.x (수업 mallapi는 구조만 참고), Java 17, Gradle, Spring Data JPA, Spring Security + JWT, MariaDB
- Frontend (frontend/): React + Vite, React Router, Redux Toolkit, axios, Tailwind CSS
- 외부: 카카오맵 JS SDK, 카카오 로컬 · 카카오모빌리티 길찾기 API, 토스페이먼츠(테스트 키), SSE(SseEmitter), Geolocation API
- 로컬 DB: 루트의 docker-compose.yml (MariaDB, transportdb)

## 문서 (작업 전에 필요한 문서만 읽을 것)
- docs/requirements.md  요구사항 (기능 ID · 담당)
- docs/status.md        배송 상태 · 전이 규칙 (가장 중요한 규칙)
- docs/erd.md           테이블 20개
- docs/api.md           API 명세 · 에러 코드
- docs/fare.md          운임 계산 규칙
- docs/collaboration.md 역할 분담 · 도메인 간 약속 · Git 규칙
- docs/roadmap.md       주차별 일정 · A~Z 단계 체크리스트
- docs/decisions.md     팀 결정 기록

## 구조 규칙
- 백엔드 패키지: config, security, domain(+enums), repository, service, controller(+advice), dto, client, scheduler, util, exception
- 수업 mallapi 코드 스타일을 따른다 (DTO 변환, PageRequestDTO/PageResponseDTO, CustomFileUtil, jwtUtil 인터셉터)
- 연관관계는 @ManyToOne(fetch = LAZY) 단방향 기본, setter 대신 change~ 메서드
- enum은 @Enumerated(EnumType.STRING)
- 프론트: api/, components/(common은 공통), layouts/, pages/{역할}/, router/, slices/, hooks/, util/

## 도메인 규칙
- 배송 상태: PENDING_PAYMENT → REQUESTED → ASSIGNED → PICKED_UP → IN_TRANSIT → ARRIVED → COMPLETED, 취소 CANCELED
- 상태 변경은 ShipmentStatus.canChangeTo()로만 검사하고, 변경마다 shipment_status_history에 기록한다
- 운임은 서버의 FareCalculator로만 계산한다. 화면이 보낸 금액은 믿지 않는다
- 결제 승인 전 payment에 저장된 금액과 요청 금액을 비교한다
- 기사 위치는 IN_TRANSIT 상태에서만 받고 보여준다

## 팀 규칙
- 담당: 팀원1 공통·인증·운영 / 팀원2 기사·차량·배차·정산 / 팀원3 배송·운임·결제 / 팀원4 진행·실시간·리뷰
- 내 도메인 파일만 수정한다. 공통 파일(SecurityConfig, JWTCheckFilter, application*.yml, build.gradle,
  ErrorCode, GlobalExceptionHandler, data.sql, router/root.jsx, package.json, layouts/, components/common/)은
  수정하지 말고 변경안을 제안만 한다
- 다른 도메인은 docs/collaboration.md의 약속된 Service 메서드로만 호출한다 (다른 도메인 Repository 직접 사용 금지)
- 작업은 develop에서 feature/{도메인}-{기능} 브랜치를 만들어 진행한다

## 작업 방식
- 코드를 바꾸기 전에 바꿀 파일 목록과 계획을 먼저 보여준다
- 한 번에 한 단계(A~Z)만 진행하고, 끝나면 실행 · 테스트 결과를 보여준다
- API 키 · 시크릿 키 · 비밀번호를 코드에 직접 쓰지 않는다 (application-local.yml, frontend/.env)
- 대화 시작 시 사용자가 자신이 몇 번 팀원인지 알려준다. 모르면 먼저 물어본다
