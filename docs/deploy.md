# 배포 (Y) — 따라 하기

- 담당: 팀원 1 (팀장). AWS 계정 · 도메인은 팀장이 직접 만든다
- 비용: **AWS 무료 플랜**으로 무료 (부트캠프 프로젝트 · 상용 아님). 유료 플랜으로 바꾸지 않는다
- 결정 기록: docs/decisions.md (배포 구조 2026-11-09)
- 파일: `deploy/` (Nginx · systemd · 운영 시드 · 배포 스크립트), `backend/src/main/resources/application-prod.yml.example`, `frontend/.env.production.example`

## 구조

```
브라우저 ──HTTPS──▶ Nginx (EC2)
                    ├─ /        → 프론트 빌드 파일 /var/www/transport (frontend/dist)
                    └─ /api/**  → Spring Boot 127.0.0.1:8080 (systemd: transport-api)
                                     ├─ DB: RDS MariaDB (EC2 보안 그룹에서만 3306)
                                     └─ 사진: /var/lib/transport/upload (EC2 디스크, 나중에 S3)
```

- 프론트와 API가 같은 주소라 CORS가 필요 없다. 프론트는 `VITE_API_HOST`를 비워서 빌드한다
- 실시간 알림(SSE)은 Nginx에서 버퍼링을 끄고 연결을 1시간까지 유지한다 (서버 heartbeat 30초)
- 브라우저 위치 API(기사 위치 공유)는 HTTPS에서만 동작해서 HTTPS가 필수다

## 0. 정한 것 (2026-11-09, docs/decisions.md)

| 항목 | 정한 것 | 이유 |
|---|---|---|
| 비용 | **AWS 무료 플랜** (6개월 · 크레딧) | 부트캠프 프로젝트라 무료로. 무료 플랜은 크레딧이 끝나거나 6개월이 지나면 요금 대신 계정이 닫힌다 |
| DB | **RDS MariaDB** 가장 작은 크기 (외부 접속 끔) | 로드맵대로 관리형 DB · 자동 백업 |
| 도메인 | 무료 DuckDNS (`이름.duckdns.org`) | 무료로 HTTPS (Let's Encrypt) |
| 배포 방식 | `deploy/deploy.sh`로 직접 | GitHub Actions 자동 배포는 나중에 |

**크레딧 계산 (대략):** EC2 가장 작은 크기 + 공인 IPv4 + RDS 가장 작은 크기면 한 달에 30달러 안팎이 크레딧에서 빠진다. 가입 크레딧 100달러면 약 3개월, 추가 크레딧(1장 3)까지 받으면 약 6개월이다. 실제 사용량은 Billing에서 확인한다.

## 1. AWS 계정 · 크레딧 · 비용 알림

1. AWS 가입 때 플랜 선택에서 **무료 플랜(Free plan)**을 고른다. 결제 수단 등록은 필요하지만, 무료 플랜에서는 크레딧을 넘어도 청구하지 않고 계정이 닫힌다. **유료 플랜(Paid plan)으로 업그레이드하지 않는다**
2. 콘솔 오른쪽 위 리전을 **아시아 태평양(서울) ap-northeast-2**로 바꾼다
3. 콘솔 홈의 추가 크레딧 활동(예산 설정 · EC2 · RDS 등)을 하면 크레딧이 늘어난다 (최대 200달러). 이 문서 순서대로 하면 대부분 하게 된다
4. Billing → **Budgets** → 비용 예산(예: 월 40달러)을 만들고 80% · 100%에서 메일 알림을 켠다. Billing → 크레딧에서 남은 크레딧을 가끔 본다
5. 루트 계정에 MFA를 켠다

## 2. EC2 만들기

1. EC2 → 인스턴스 시작: **Ubuntu Server 24.04 LTS**, 가장 작은 크기(t3.micro 등, 크레딧 아끼기), 스토리지 20GB
2. 키 페어를 새로 만들고 `.pem`을 내 PC에 보관한다 (Git · 채팅에 올리지 않기)
3. 보안 그룹 `transport-web`
   - SSH 22: **내 IP만**
   - HTTP 80 · HTTPS 443: 모든 곳 (0.0.0.0/0)
4. 탄력적 IP를 할당해 인스턴스에 연결한다 (재시작해도 주소가 그대로)

## 3. RDS(MariaDB) 만들기

1. 파라미터 그룹(mariadb11.4)을 만들고 `time_zone`=Asia/Seoul, `character_set_server`=utf8mb4, `collation_server`=utf8mb4_unicode_ci
2. RDS → 데이터베이스 생성: MariaDB **11.4**(로컬 Docker와 같은 버전), 가장 작은 크기(db.t4g.micro 등), 스토리지 20GB, 위 파라미터 그룹
3. 초기 데이터베이스 이름 `transportdb`, 마스터 사용자 `admin` (비밀번호는 팀장만 보관)
4. **퍼블릭 액세스: 아니요**. 보안 그룹 `transport-db`: MariaDB 3306을 **`transport-web` 보안 그룹에서만** 허용
5. 크레딧 아끼기: **단일 AZ**(다중 AZ 끔), 자동 백업 보존 1일, Performance Insights · 확장 모니터링 끔, 스토리지 자동 확장 끔
6. 만들어지면 엔드포인트(`....rds.amazonaws.com`)를 적어 둔다

## 4. 도메인 연결 (DuckDNS 예)

1. duckdns.org에 로그인해 하위 도메인(예: `transport-team`)을 만든다
2. current ip에 EC2 탄력적 IP를 넣고 update → `transport-team.duckdns.org`가 EC2를 가리킨다
3. 아래에서 `YOUR_DOMAIN`은 이 도메인으로 바꿔 읽는다

## 5. 서버 처음 설정 (한 번만)

내 PC(Git Bash, 저장소 루트)에서 설정 파일을 서버로 올리고 접속한다.

```bash
scp -i ~/.ssh/transport.pem -r deploy backend/src/main/resources/application-prod.yml.example ubuntu@YOUR_DOMAIN:~/
ssh -i ~/.ssh/transport.pem ubuntu@YOUR_DOMAIN
```

### 5-1. 패키지 · 스왑 · 폴더

```bash
sudo apt update && sudo apt -y upgrade
sudo apt -y install openjdk-21-jre-headless nginx certbot mariadb-client apache2-utils
sudo timedatectl set-timezone Asia/Seoul

# 메모리 1GB 서버라 스왑 2GB를 만든다
sudo fallocate -l 2G /swapfile && sudo chmod 600 /swapfile
sudo mkswap /swapfile && sudo swapon /swapfile
echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab

# 백엔드 실행 계정 · 폴더
sudo useradd --system --home /opt/transport --shell /usr/sbin/nologin transport
sudo mkdir -p /opt/transport/config /var/lib/transport/upload /var/www/transport
sudo chown -R transport:transport /opt/transport /var/lib/transport
```

### 5-2. DB 사용자 (마스터 계정은 앱에 쓰지 않는다)

```bash
mariadb -h RDS_엔드포인트 -u admin -p
```
```sql
CREATE USER 'transport'@'%' IDENTIFIED BY '앱용 비밀번호';
GRANT ALL PRIVILEGES ON transportdb.* TO 'transport'@'%';
FLUSH PRIVILEGES;
```

### 5-3. 운영 설정 (비밀값은 이 파일에만)

```bash
sudo cp ~/application-prod.yml.example /opt/transport/config/application-prod.yml
sudo chown transport:transport /opt/transport/config/application-prod.yml
sudo chmod 600 /opt/transport/config/application-prod.yml
openssl rand -base64 48          # JWT secret으로 쓸 값
sudo nano /opt/transport/config/application-prod.yml
```
- `DB_HOST` → RDS 엔드포인트, `password` → 5-2의 앱용 비밀번호
- `jwt.secret` → 위에서 만든 값, 카카오 REST 키 · 토스 시크릿 키 → 팀 비공개 메모 값
- `YOUR_DOMAIN` → 도메인

### 5-4. 백엔드 서비스 등록

```bash
sudo cp ~/deploy/transport-api.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable transport-api      # 아직 jar가 없어서 시작은 6장에서
```

### 5-5. HTTPS 인증서 (Let's Encrypt)

```bash
sudo certbot certonly --standalone -d YOUR_DOMAIN -m 팀장이메일 --agree-tos \
  --pre-hook "systemctl stop nginx" --post-hook "systemctl start nginx"
sudo certbot renew --dry-run             # 자동 갱신 확인 (90일마다)
```

### 5-6. Nginx

```bash
sed 's/YOUR_DOMAIN/실제도메인/g' ~/deploy/nginx/transport.conf | sudo tee /etc/nginx/sites-available/transport > /dev/null
sudo ln -sf /etc/nginx/sites-available/transport /etc/nginx/sites-enabled/transport
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

## 6. 첫 배포

### 6-1. 첫 기동만 테이블 만들기

운영 설정은 `ddl-auto: validate`라 테이블이 없으면 시작되지 않는다. **첫 기동만** 테이블을 만든다.

1. 서버의 `application-prod.yml`에서 `ddl-auto: validate` → `update`
2. 내 PC에서 `frontend/.env.production.example`을 `frontend/.env.production.local`로 복사하고 카카오 JS 키 · 토스 클라이언트 키를 채운다 (`VITE_API_HOST`는 비워 둔다)
3. 배포 스크립트 실행 (Git Bash, 저장소 루트):
   ```bash
   DEPLOY_HOST=ubuntu@YOUR_DOMAIN DEPLOY_KEY=~/.ssh/transport.pem ./deploy/deploy.sh
   ```
4. "백엔드 정상 (health 200)"이 나오면 서버에서 `update` → `validate`로 되돌리고 `sudo systemctl restart transport-api`
5. 다시 정상으로 뜨면 테이블이 엔티티와 맞다는 뜻이다. 엔티티를 바꾸는 PR이 병합되면 배포 전에 6-1을 다시 한다

### 6-2. 운영 시드 · 관리자 계정

```bash
mariadb -h RDS_엔드포인트 -u transport -p transportdb < ~/deploy/seed-prod.sql
```
`deploy/seed-prod.sql` 맨 아래 설명대로 관리자 비밀번호 해시를 만들어 관리자 계정을 한 번 넣는다. **테스트 계정(비밀번호 1234)은 운영에 넣지 않는다.**

## 7. 확인

- [ ] `https://YOUR_DOMAIN` 접속 · 자물쇠 표시, `http://`는 https로 넘어감
- [ ] 관리자 로그인 → 대시보드, 새로고침 · 주소 직접 입력(`/admin/inquiries`)도 화면이 뜸
- [ ] `https://YOUR_DOMAIN/swagger-ui.html` · `/v3/api-docs`가 열리지 않음 (운영에서 닫힘)
- [ ] 고객 가입 → 로그인 → 내 정보 · 1:1 문의
- [ ] 휴대폰으로 접속해 위치 권한 요청이 뜨는지 (기사 위치 공유가 생기면)
- [ ] `journalctl -u transport-api -n 50`에 에러가 없음

## 8. 외부 서비스를 배포 주소로

- 카카오 디벨로퍼스 → 팀 앱 → 플랫폼 → Web 사이트 도메인에 `https://YOUR_DOMAIN` 추가
- 토스페이먼츠 결제 성공 · 실패 URL을 `https://YOUR_DOMAIN/...`으로 (결제 화면이 생긴 뒤, 팀원 3)
- 팀 메신저에 배포 주소와 테스트 방법 공지

## 9. 다음 배포부터

develop에 병합된 뒤 내 PC(develop 최신)에서:

```bash
DEPLOY_HOST=ubuntu@YOUR_DOMAIN DEPLOY_KEY=~/.ssh/transport.pem ./deploy/deploy.sh
```

엔티티(테이블)가 바뀐 병합이 있으면 6-1처럼 한 번 `update`로 띄운 뒤 `validate`로 돌린다.

## 10. 문제 해결

| 증상 | 확인 |
|---|---|
| 502 Bad Gateway | 백엔드가 꺼짐: `sudo systemctl status transport-api`, `journalctl -u transport-api -n 100` |
| 백엔드가 바로 꺼짐 | 로그에서 `Schema-validation`(테이블 불일치 → 6-1) · DB 접속 실패(보안 그룹 · 비밀번호) 확인 |
| 알림(SSE)이 늦게 옴 | Nginx 설정의 `/api/notifications/subscribe` 블록이 있는지 (`proxy_buffering off`) |
| 화면은 뜨는데 API가 401 · CORS 에러 | 프론트를 `VITE_API_HOST=`(빈 값)으로 빌드했는지 |
| 디스크 부족 | `df -h`, 사진 폴더 `/var/lib/transport/upload` 크기 |

## 11. 비용 · 정리

- Budgets 알림 메일을 받으면 바로 확인한다. 공인 IPv4(탄력적 IP)는 인스턴스에 연결돼 있어도 크레딧이 빠진다
- 무료 플랜이 끝나기 전(6개월 · 크레딧 소진)에 계정이 닫혀도 남도록 DB 덤프를 받아 둔다:
  ```bash
  mariadb-dump -h RDS_엔드포인트 -u transport -p --single-transaction transportdb > transportdb-$(date +%F).sql
  ```
- 발표 · 제출이 끝나면 덤프를 받고 EC2 · RDS · 탄력적 IP를 삭제한다 (연결되지 않은 탄력적 IP도 크레딧이 빠진다)
