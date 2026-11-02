# 운송서비스 운용 사이트 (transport)

화물 운송 접수 · 결제 · 배차 · 실시간 추적 · 정산 웹 서비스 — Spring Boot + React, 4인 팀 프로젝트

## 팀

| 담당 | 이름 | 도메인 | 공통 책임 |
|---|---|---|---|
| 팀원 1 (팀장) | | 공통 · 인증 · 운영 | 일정 관리, 배포 |
| 팀원 2 | | 기사 · 차량 · 배차 · 정산 | Git · ERD · 시드 데이터 관리 |
| 팀원 3 | | 배송 접수 · 운임 · 결제 | API 문서(Swagger) 관리 |
| 팀원 4 | | 배송 진행 · 실시간 · 리뷰 | UI 통일 관리 |

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
#   백엔드: STS에서 Spring Boot App 실행 (8080)
#   프론트: cd frontend && npm ci && npm run dev (5173)
```

- 이미 PC에 MariaDB가 3306으로 켜져 있으면 `docker-compose.yml`의 포트를 `"3307:3306"`으로 바꾸고 `application-local.yml`의 URL도 3307로 바꾼다
- Docker를 못 쓰면 MariaDB를 직접 설치하고 `docker-compose.yml`에 적힌 DB 이름 · 계정 · 비밀번호로 똑같이 만든다
- STS 인코딩: Window → Preferences → General → Workspace → Text file encoding을 **UTF-8**로 (한글 깨짐 방지)
- 카카오 · 토스 키는 Git이 아니라 팀 비공개 메모로 받는다

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

## 테스트 계정 (data.sql, 비밀번호 모두 1234)

| 역할 | 이메일 |
|---|---|
| 관리자 | admin@transport.test |
| 고객 | customer@transport.test |
| 기사 (승인됨) | driver@transport.test |
