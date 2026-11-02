# 로드맵 — 10주 · 4인 병렬

알파벳(A~Z)은 의존 순서이고, 시간 순서는 아래 주차표를 따른다. 체크 항목은 GitHub 이슈로 옮겨 쓴다.

## 주차표

| 주차 | 팀원 1 (공통·인증·운영) | 팀원 2 (기사·배차·정산) | 팀원 3 (배송·운임·결제) | 팀원 4 (진행·실시간·리뷰) | 완료 기준 |
|---|---|---|---|---|---|
| 1 | 전원 A~D: 역할 · 규칙 확정, 요구사항, 상태 확정, 화면 목록 | ← | ← | ← | 요구사항 · 상태 · 화면 목록 |
| 2 | 저장소 · 템플릿 · CLAUDE.md (H), 공통 골격 1차 (I) | ERD 통합 (E), 시드 초안 | API 명세 · Swagger (F), 카카오 · 토스 키 (G) | 상태 · 알림 · 위치 설계, UI 규칙 | 전원 빈 프로젝트 실행 |
| 3 | 가입 · 약관 · JWT (J), 레이아웃 · 로그인 (K) | Driver · Vehicle · VehicleType + 관리자 API (L) | Shipment · CargoType 공유, 거리 · 운임 계산 (M) | 알림 엔티티 · SSE 구독, 이력, FileStorage | 역할별 로그인, 운임 테스트 통과 |
| 4 | 공지 · FAQ · 문의 (V), 마이페이지, 회원 · 약관 관리 | 차종 · 차량 · 기사 화면, 근무 일정 (L) | 신청 화면 · 신청 API → 결제대기 (N) | 상태 변경 · 이력 · 타임라인 (P) | 신청 → 결제대기 저장 |
| 5 | 공통 컴포넌트 · 권한 정리, 대시보드 쿼리 설계 | 배차 후보 · 배정 · 해제 (O) | 토스 결제 승인 · 재시도 · 자동 취소 (N) | 기사 처리 화면, 사진 (Q) | 결제 → 접수 → 배차 |
| 6 | 통합 이슈 · 보안 점검 | 재배차 · 일정 조건 검증 | 취소 · 환불, 내 배송 목록 · 상세 | 알림 연결 (R) | **MVP: 신청 → 완료 통합** |
| 7 | 대시보드 · 통계 (T) | 월 정산 (U), 기사 실적 | 관리자 검색 · 결제 내역 (S) | 실시간 위치 (R) | 지도에서 기사 이동 |
| 8 | 1차 배포 · HTTPS (Y) | 평점 정렬 반영 | 더미 데이터, N+1 점검 | 리뷰 · 평점 (Q) | 배포 URL 전체 흐름 |
| 9 | 전원 W~Y: 테스트, UI 통일(팀원 4 주도), 최종 배포 | ← | ← | ← | 배포 URL 시연 통과 |
| 10 | 전원 Z: README · 트러블슈팅 · 발표 · 시연 영상, 버퍼 | ← | ← | ← | 제출물 완성 |

밀릴 때 미루는 순서: 공지·FAQ → 정산 → 리뷰 → 통계 → 위치 추적 (지도 · 운임 · 결제 · 알림은 지킨다)

## Phase 1 · 기획 (1주차 · 전원)

### A. 팀 구성 · 목표 · 범위 — 전원
- [ ] README 팀 표에 이름 채우기
- [ ] 포함 · 제외 범위 확정 (requirements.md 상단)
- [ ] collaboration.md 규칙 합의 (리뷰 짝, 회의 시간, 메신저 채널)
- [ ] 제출일 · 발표일 확인, 주차표 조정

### B. 요구사항 — 전원, 취합 팀원 1
- [ ] 각자 도메인 요구사항 검토 · 보완 (requirements.md)
- [ ] 예외 상황 정리

### C. 배송 상태 — 전원
- [ ] status.md 상태 8개 · 전이 규칙 확정
- [ ] 고객 · 관리자 · 기사 역할을 나눠 말로 시연

### D. 화면 목록 · 와이어프레임 — 전원
- [ ] 화면 목록 · 담당 확정, 와이어프레임
- [ ] 화면마다 호출 API 메모, 기사 화면은 모바일 기준

## Phase 2 · 설계 · 준비 (2주차)

### E. ERD — 전원, 취합 팀원 2
- [ ] erd.md 기준 ERDCloud 작성, FK · UQ · enum 확정
- [ ] data.sql 검토

### F. API 명세 — 전원, 취합 팀원 3
- [ ] api.md 요청 / 응답 예시 보강, 에러 코드 확정
- [ ] Swagger(springdoc, Boot 버전에 맞는 버전), Postman 팀 워크스페이스
- [ ] collaboration.md 약속 메서드 이름 · 파라미터 확정

### G. 외부 서비스 — 팀원 3 · 4
- [ ] (3) 카카오 팀 앱 1개, JS SDK 도메인 http://localhost:5173, 카카오맵 활성화(무료 쿼터는 계정당 첫 앱), 키 공유
- [ ] (3) 토스페이먼츠 개발자센터 가입 → 테스트 API 개별 연동 키(클라이언트 · 시크릿)
- [ ] (3) 카카오모빌리티 길찾기 사용 조건 · 쿼터 확인
- [ ] (4) HTTPS 터널 도구로 휴대폰 위치 권한 확인

### H. 개발 환경 · 저장소 — 팀원 1, Git 팀원 2
- [ ] (1) backend/ Spring Initializr, frontend/ Vite React
- [ ] (2) 저장소 · 초대 · develop 기본 브랜치 · Projects 보드 · 라벨
- [ ] (전원) clone → docker compose up → 설정 파일 복사 → 실행 → 5173에서 8080 호출 성공
- [ ] (전원) 연습 PR 1개씩

## Phase 3 · 공통 기반 (2~3주차 · 팀원 1)

### I. 백엔드 공통 골격 — 팀원 1 (2주차)
- [ ] BaseEntity(JPA Auditing), PageRequestDTO / PageResponseDTO
- [ ] CustomException + 도메인별 구역 ErrorCode + GlobalExceptionHandler
- [ ] CORS, 시간대, Swagger 기본, 임시 전체 허용 보안

### J. 회원 · 인증 · 약관 — 팀원 1 (3주차)
- [ ] Member · Term · TermAgreement, 가입(필수 약관 검사, BCrypt)
- [ ] 기사 가입 시 팀원 2의 DriverService로 Driver(PENDING) 생성
- [ ] 로그인 · 재발급 · JWTCheckFilter, URL 권한 + @PreAuthorize
- [ ] 임시 전체 허용 → 실제 권한 전환일 미리 공지

### K. 프론트 공통 — 팀원 1 + UI 팀원 4 (3주차)
- [ ] (1) 레이아웃 4종, 보호 라우트, loginSlice + 쿠키, axios 인터셉터
- [ ] (1) 로그인 · 가입(약관) 화면
- [ ] (4) 공통 컴포넌트: Button · Input · Modal · Table · Pagination · Loading · StatusBadge

## Phase 4 · 핵심 기능 (3~6주차)

### L. 차종 · 차량 · 기사 · 근무 일정 — 팀원 2
- [ ] Driver · Vehicle · VehicleType (VehicleType 먼저 공유)
- [ ] 관리자: 기사 승인 · 중지, 차종 · 요금, 차량 · 담당 기사
- [ ] DriverSchedule 달력 화면

### M. 화물 종류 · 거리 · 운임 — 팀원 3
- [ ] Shipment · CargoType · ShipmentStatus 먼저 공유
- [ ] KakaoLocalClient, KakaoDirectionsClient (실패 시 하버사인 × 1.3)
- [ ] FareCalculator + fare.md 예시 3개 테스트, POST /api/fares/estimate

### N. 배송 신청 · 지도 · 결제 — 팀원 3
- [ ] POST /api/shipments → PENDING_PAYMENT + payment READY
- [ ] 신청 화면: 우편번호 팝업, 지도 마커(JS SDK services), 차종 추천, 0.5초 디바운스 예상 운임
- [ ] 토스 결제창 → success 페이지 → /api/payments/confirm (금액 비교) → REQUESTED
- [ ] fail 페이지 · 재시도, @Scheduled 30분 자동 취소, 승인 후 저장 실패 시 결제 취소
- [ ] local 전용 결제 건너뛰기 API

### O. 배차 — 팀원 2
- [ ] 후보: ACTIVE + 근무 일정 안 + 차량 AVAILABLE + 적재량 ≥ 무게 + 요청 차종 이상 + 당일 < 3건
- [ ] 정렬: 당일 건수 → 적재 여유 → (8주차) 평점
- [ ] 배정 / 해제, @Version 충돌 처리, getActiveDriver 제공, 기사 배차 목록

### P. 상태 변경 · 이력 — 팀원 4
- [ ] StatusHistoryService.record 먼저 공유
- [ ] PATCH /status: 배정 기사 확인 → canChangeTo → 변경 → 이력
- [ ] 타임라인 컴포넌트, 기사 화면 “다음 단계” 버튼, JUnit 테스트

## Phase 5 · 확장 기능 (4~8주차)

### Q. 사진 · 리뷰 — 팀원 4
- [ ] ShipmentImage, CustomFileUtil, FileStorage 인터페이스(로컬 / S3)
- [ ] PICKUP · RECEIPT 필수 검사, `<input accept="image/*" capture="environment">`
- [ ] (8주차) Review: 완료 건 1회, 평균 평점 메서드 제공

### R. 실시간 알림 · 위치 — 팀원 4
- [ ] SseEmitter 관리 · heartbeat, event-source-polyfill
- [ ] 알림 저장 + 전송, @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
- [ ] useLocationShare: IN_TRANSIT만 watchPosition, 30초 전송, clearWatch
- [ ] 서버 location 이벤트 → 고객 지도 마커, “n초 전”, 30일 삭제 스케줄러

### S. 검색 · 결제 내역 — 팀원 3 (+ 전원 N+1 점검)
- [ ] 관리자 통합 검색(QueryDSL), 결제 · 환불 내역, 화물 종류 관리
- [ ] (전원) 자기 목록 N+1 · 정렬 · 페이징 · URL 쿼리스트링

### T. 대시보드 · 통계 — 팀원 1
- [ ] KPI(오늘 건수, 이번 달 매출, 평균 평점), 일별 · 차종별 차트
- [ ] 다른 도메인 데이터는 담당자 조회 메서드로

### U. 월 정산 — 팀원 2
- [ ] COMPLETED(환불 제외) 기사별 합산 → 수수료 → 지급액, 중복 생성 방지, 지급 처리
- [ ] 검증: 42건, 4,200,000원, 수수료 10% → 3,780,000원

### V. 공지 · FAQ · 문의 — 팀원 1
- [ ] 공지 · FAQ · 약관 관리, 문의 답변 → 알림

## Phase 6 · 마무리 (8~10주차)

### W. 테스트 — 전원
- [ ] 도메인별 단위 테스트, 신청 → 완료 → 리뷰 통합 테스트
- [ ] 결제: 성공 · 실패 · 창 닫기 · 30분 만료 · 환불 / 권한 403
- [ ] 실제 휴대폰 포함 역할별 시연

### X. UI 다듬기 — 전원, 주도 팀원 4
- [ ] 화면 점검표 → 각자 수정, 로딩 · 빈 목록 · 에러 · 금액 · 배지 통일

### Y. 배포 — 팀원 1
- [ ] AWS 계정(무료 플랜 6개월) · Budgets, RDS · S3, EC2 prod 프로필(ddl-auto validate)
- [ ] Nginx + /api 프록시(SSE 버퍼링 off), 도메인 + HTTPS(필수)
- [ ] 카카오 도메인 · 토스 success/fail URL · CORS를 배포 주소로

### Z. 문서 · 발표 — 전원, 취합 팀원 1
- [ ] README, 각자 트러블슈팅 2개+, decisions.md 정리
- [ ] 발표: 1 구조 · 보안 · 배포 / 3 신청 · 결제 / 2 배차 · 정산 / 4 실시간 · 리뷰, 시연 영상
