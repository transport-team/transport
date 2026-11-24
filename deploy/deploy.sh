#!/usr/bin/env bash
# 배포 스크립트 — 내 PC(Git Bash)에서 빌드해 EC2로 올리고 백엔드를 다시 시작한다 (docs/deploy.md 6 · 9장)
# 사용: DEPLOY_HOST=ubuntu@YOUR_DOMAIN DEPLOY_KEY=~/.ssh/transport.pem ./deploy/deploy.sh
# 준비: frontend/.env.production.local (frontend/.env.production.example 복사, VITE_API_HOST는 빈 값)
set -euo pipefail

: "${DEPLOY_HOST:?DEPLOY_HOST=ubuntu@도메인 을 지정하세요}"
: "${DEPLOY_KEY:?DEPLOY_KEY=EC2 키 파일(.pem) 경로를 지정하세요}"

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
SSH_OPTS=(-i "$DEPLOY_KEY" -o StrictHostKeyChecking=accept-new)

# 프론트는 같은 주소의 /api를 불러야 한다 (빈 값이 아니면 로컬 주소로 빌드될 수 있다)
if ! grep -qE '^VITE_API_HOST=[[:space:]]*$' "$ROOT/frontend/.env.production.local" 2>/dev/null; then
  echo "frontend/.env.production.local에 'VITE_API_HOST=' (빈 값)이 있어야 해요 — frontend/.env.production.example을 복사하세요" >&2
  exit 1
fi

echo "== 1/4 백엔드 빌드"
(cd "$ROOT/backend" && ./gradlew clean bootJar -q)
JAR="$ROOT/backend/build/libs/backend-0.0.1-SNAPSHOT.jar"

echo "== 2/4 프론트 빌드"
(cd "$ROOT/frontend" && npm ci --silent && npm run build)
WORK="$(mktemp -d)"
tar -C "$ROOT/frontend/dist" -czf "$WORK/transport-dist.tgz" .

echo "== 3/4 서버로 올리기"
scp "${SSH_OPTS[@]}" "$JAR" "$DEPLOY_HOST:/tmp/backend.jar"
scp "${SSH_OPTS[@]}" "$WORK/transport-dist.tgz" "$DEPLOY_HOST:/tmp/transport-dist.tgz"

echo "== 4/4 교체 · 재시작 · 확인"
ssh "${SSH_OPTS[@]}" "$DEPLOY_HOST" 'bash -s' <<'REMOTE'
set -euo pipefail
sudo install -o transport -g transport -m 644 /tmp/backend.jar /opt/transport/backend.jar
sudo rm -rf /var/www/transport.new /var/www/transport.old
sudo mkdir -p /var/www/transport.new
sudo tar -xzf /tmp/transport-dist.tgz -C /var/www/transport.new
if [ -d /var/www/transport ]; then sudo mv /var/www/transport /var/www/transport.old; fi
sudo mv /var/www/transport.new /var/www/transport
sudo systemctl restart transport-api
for i in $(seq 1 30); do
  if curl -fs http://127.0.0.1:8080/api/health > /dev/null; then
    echo "백엔드 정상 (health 200)"
    exit 0
  fi
  sleep 2
done
echo "백엔드가 60초 안에 뜨지 않았어요 — journalctl -u transport-api -n 100 으로 확인" >&2
exit 1
REMOTE

echo "배포 끝: https://${DEPLOY_HOST#*@}"
