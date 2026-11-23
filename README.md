# 운송서비스 운용 사이트 (transport)

화물 운송 접수 · 결제 · 배차 · 실시간 추적 · 정산 웹 서비스 — Spring Boot + React, 4인 팀 프로젝트

## 팀

| 담당 | 이름 | 도메인 | 공통 책임 |
|---|---|---|---|
| 팀원 1 (팀장) | 이민현 | 공통 · 인증 · 운영 | 일정 관리, 배포 |
| 팀원 2 | 박서현 | 기사 · 차량 · 배차 · 정산 | Git · ERD · 시드 데이터 관리 |
| 팀원 3 | 이시은 | 배송 접수 · 운임 · 결제 | API 문서(Swagger) 관리 |
| 팀원 4 | 남궁현석 | 배송 진행 · 실시간 · 리뷰 | UI 통일 관리 |

## 기능 (2026-11-23 저녁 develop `9618ecf` 기준, #144까지 — 마감 때 다시 정리)

| 역할 | 화면까지 되는 것 | 남은 것 |
|---|---|---|
| 공통 | 회원가입(약관 동의) · 로그인(JWT) · 마이페이지, 공지 · FAQ · 문의, 알림(SSE, 헤더 종, 탭 여러 개), 리뷰(작성 고객 이름은 김*수처럼 가림), 배송 상태 이름 · 배지 통일 | 메인 화면 서비스 소개(#147 리뷰 중) |
| 고객 | 배송 신청(주소 검색 · 지도 마커 · 서버 운임 계산 · 토스 테스트 결제), 지원한 기사 고르기(평점 · 리뷰 보고 확정 · 거절), 내 배송 목록 · 상세(타임라인 · 실시간 기사 위치 · 증빙 사진 · 리뷰 쓰기), 기사 배송 완료 뒤 수령 확정, 결제 재시도, 30분 미결제 자동 취소, 배차 전 취소 · 환불 | |
| 기사 | 가입 → 관리자 승인 뒤 로그인, 차량 등록 신청 · 내 차량, 운송 요청 목록(차종 필터) · 지원, 내 배차, 상차 → 운송 → 도착 → 배송 완료 등록(사진 · 운송 중 위치 공유) → 고객 수령 확정, 휴무 달력 · 근무 가능 시간, 실적(월별 건수 · 운임 · 평균 평점 · 리뷰) | 실적 화면의 정산 상태 표시 |
| 관리자 | 대시보드(오늘 · 이번 달 숫자, 차트, 기사 순위), 기사 승인 · 중지(리뷰 · 보유 차량 보기), 차량 · 차량 등록 · 삭제 신청, 차종 · 요금 관리, 배송 검색(기사 조건 포함) · 상세 · 배차 취소(재배차) · 배송 취소(환불), 결제 · 환불 내역, 월 정산 · 지급 확인, 화물 종류, 회원, 약관 · 공지 · FAQ · 문의 답변 | 배송 상세의 결제 상태 한글 표시(#143) |

자세한 진행은 [docs/roadmap.md](docs/roadmap.md), 요구사항은 [docs/requirements.md](docs/requirements.md)

## 폴더 구조

```
transport/
├─ backend/            Spring Boot (STS)
├─ frontend/           React (VS Code)
├─ docs/               요구사항 · 상태 · ERD · API · 운임 · 협업 · 일정 · 결정 기록
├─ docker-compose.yml  로컬 MariaDB
├─ CLAUDE.md           Claude Code 공용 규칙
└─ .github/            PR · 이슈 템플릿
```

## 처음 세팅 (팀장 1회)

1. GitHub Organization과 저장소를 만들고 팀원 3명을 Write 권한으로 초대한다
2. 이 묶음의 파일을 저장소 루트에 넣는다 (숨김 파일 `.gitignore`, `.gitattributes`, `.editorconfig`, `.github/` 포함)
3. STS에서 Spring Initializr로 **`backend/` 폴더에** 프로젝트를 만든다
   - 수업과 같은 Spring Boot · Java 버전, Gradle
   - 의존성: Spring Web, Spring Data JPA, Spring Security, Validation, Lombok, MariaDB Driver
   - 자동으로 생긴 `application.properties`는 지우고, 이 묶음의 `application.yml`을 쓴다
4. VS Code에서 **`frontend/` 폴더에** Vite React 프로젝트를 만든다 (`npm create vite@latest frontend -- --template react`)
   - 이미 있는 `frontend/.env.example`은 그대로 둔다
5. `main`, `develop` 브랜치를 push하고 기본 브랜치를 `develop`으로 바꾼다
6. 브랜치 보호: 비공개 저장소는 무료 플랜에서 브랜치 보호를 쓸 수 없다. 공개 저장소로 하거나, 학생 혜택 등으로 Pro를 쓰거나, "main · develop 직접 push 금지"를 규칙으로 지킨다

## 처음 세팅 (팀원 각자)

```bash
# 0) 한 번만: 커밋 작성자 설정 (GitHub 계정 이메일)
git config --global user.name "이름"
git config --global user.email "github계정@email.com"

# 1) 받기
git clone <저장소 주소>
cd transport
git switch develop

# 2) DB 띄우기 (Docker Desktop 실행 후)
docker compose up -d

# 3) 설정 파일 복사 후 키 채우기 (이 파일들은 Git에 올라가지 않음)
cp backend/src/main/resources/application-local.yml.example backend/src/main/resources/application-local.yml
cp frontend/.env.example frontend/.env

# 4) 실행
#   백엔드: STS에서 Spring Boot App 실행 (8080) — 처음 한 번 아래 'STS 파라미터 이름' 설정
#   프론트: cd frontend && npm ci && npm run dev (5173) — 브라우저는 http://localhost:5173 으로 연다
```

- 이미 PC에 MariaDB가 3306으로 켜져 있으면 `docker-compose.yml`의 포트를 `"3307:3306"`으로 바꾸고 `application-local.yml`의 URL도 3307로 바꾼다
- Docker를 못 쓰면 MariaDB를 직접 설치하고 `docker-compose.yml`에 적힌 DB 이름 · 계정 · 비밀번호로 똑같이 만든다
- STS 인코딩: Window → Preferences → General → Workspace → Text file encoding을 **UTF-8**로 (한글 깨짐 방지)
- STS 파라미터 이름: `backend` 우클릭 → Properties → Java Compiler → Classfile Generation의 **'Store information about method parameters (usable via reflection)'** 체크 → Project → Clean. 안 하면 STS에서 직접 실행할 때 이름 없이 쓴 `@PathVariable` · `@RequestParam` API가 오류를 낸다 (#88). IntelliJ는 Build and run using을 Gradle로 둔다
- 카카오 · 토스 키는 Git이 아니라 팀 비공개 메모로 받는다
- 카카오 지도는 `http://localhost:5173`으로 열어야 뜬다. 카카오 콘솔(앱 → 플랫폼 키 → JavaScript 키 → JavaScript SDK 도메인)에 이 주소만 등록돼 있어서 `127.0.0.1:5173`이나 다른 포트로 열면 지도가 안 뜬다

## DB 공유 방식

DB 자체는 공유하지 않는다. 각자 로컬에 같은 DB를 만든다.

- 테이블: JPA 엔티티가 자동 생성 (`ddl-auto: update`)
- 초기 데이터: `backend/src/main/resources/data.sql` (엔티티가 생긴 뒤 `application-local.yml`에서 `spring.sql.init.mode: always`로 켠다)
- 엔티티를 바꾸는 PR이 병합되면 메신저에 공지 → 다른 팀원은 pull 후 서버 재실행
- 컬럼을 지운 경우: `docker compose down -v && docker compose up -d`로 DB를 초기화하고 다시 실행

## 작업 흐름

1. 이슈 확인 → `develop`에서 브랜치 생성: `git switch -c feature/dispatch-candidate`
2. 작업 · 커밋: `feat: 배차 후보 조회 API (#12)`
3. 하루 1번 이상 develop 받아오기: `git pull origin develop`
4. push → PR(develop 대상) → 짝 리뷰어 1명 승인 → Squash merge → 브랜치 삭제

자세한 규칙: [docs/collaboration.md](docs/collaboration.md) · 일정: [docs/roadmap.md](docs/roadmap.md)

## 테스트

```bash
# 백엔드 (Docker의 MariaDB가 켜져 있어야 한다 — 실제 DB로 확인하는 테스트는 끝나면 롤백)
cd backend && ./gradlew test        # Windows 명령 프롬프트는 gradlew.bat test

# 프론트
cd frontend && npm run lint && npm run build
```

- PR을 올리면 GitHub Actions(CI)가 같은 검사(백엔드 테스트 · 프론트 lint · 빌드)를 돌린다. 실패하면 병합 전에 고친다
- PC를 다시 켠 뒤에는 Docker Desktop부터 켠다. 꺼져 있으면 DB를 쓰는 테스트가 `localhost:3307` 연결 거부(`Connection refused`)로 한꺼번에 실패한다
- 전체 흐름은 `ShipmentFlowIntegrationTest`가 확인한다: 신청 → 결제 → 배차 → 완료 → 리뷰 → 관리자 대시보드 (카카오 길찾기 · 토스 승인만 가짜)

## 테스트 계정 (data.sql, 비밀번호 모두 1234)

| 역할 | 이메일 |
|---|---|
| 관리자 | admin@transport.test |
| 고객 | customer@transport.test |
| 기사 (승인됨) | driver@transport.test |
