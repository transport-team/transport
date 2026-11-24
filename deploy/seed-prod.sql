-- 운영(prod) 기본 데이터 — 첫 배포 때 한 번 실행한다 (docs/deploy.md 6장)
-- data.sql에서 운영에 필요한 것만 가져왔다: 약관 · FAQ · 차종 · 화물 종류 (data.sql을 바꾸면 여기도 맞춘다)
-- 테스트 계정(비밀번호 1234) · 테스트 기사 · 차량 · 근무 일정은 넣지 않는다 — 운영에 들어가면 누구나 로그인할 수 있다
-- INSERT IGNORE라 다시 실행해도 중복으로 들어가지 않는다
-- 실행 (EC2에서): mariadb -h DB_HOST -u transport -p transportdb < seed-prod.sql

-- 약관: 내용은 배포 뒤 관리자 화면(약관 → 새 버전으로 개정)에서 채운다
INSERT IGNORE INTO term (id, title, content, required, version, active, created_at, updated_at) VALUES
 (1, '서비스 이용약관',        '서비스 이용약관 내용 (작성 예정)',        1, 'v1.0', 1, NOW(), NOW()),
 (2, '개인정보 수집 · 이용 동의', '개인정보 수집 · 이용 동의 내용 (작성 예정)', 1, 'v1.0', 1, NOW(), NOW()),
 (3, '마케팅 정보 수신 동의',    '마케팅 정보 수신 동의 내용 (작성 예정)',    0, 'v1.0', 1, NOW(), NOW());

INSERT IGNORE INTO faq (id, category, question, answer, sort_order, active, created_at, updated_at) VALUES
 (1, '결제', '결제하지 않으면 어떻게 되나요?', '신청 후 30분 안에 결제하지 않으면 자동으로 취소됩니다.', 1, 1, NOW(), NOW()),
 (2, '배송', '배송 중 기사 위치를 볼 수 있나요?', '운송중 상태일 때 배송 상세 화면 지도에서 볼 수 있습니다.', 2, 1, NOW(), NOW());

-- 차종 · 요금 (docs/fare.md)
INSERT IGNORE INTO vehicle_type (id, code, name, max_load_kg, base_fare, base_distance_km, per_km_fare, night_rate, weekend_rate, heavy_rate, active, created_at, updated_at) VALUES
 (1, 'SMALL',  '소형 (다마스 · 라보)', 500,   30000, 10, 1000, 20, 10, 10, 1, NOW(), NOW()),
 (2, 'TON1',   '1톤',               1000,  50000, 10, 1500, 20, 10, 10, 1, NOW(), NOW()),
 (3, 'TON2_5', '2.5톤',             2500,  90000, 10, 2200, 20, 10, 10, 1, NOW(), NOW()),
 (4, 'TON5',   '5톤',               5000, 150000, 10, 3000, 20, 10, 10, 1, NOW(), NOW());

-- 화물 종류
INSERT IGNORE INTO cargo_type (id, name, extra_fee, handling_note, active, created_at, updated_at) VALUES
 (1, '일반',       0,     NULL,                                   1, NOW(), NOW()),
 (2, '파손주의',   5000,  '완충재 포장 확인, 적재 시 위에 다른 짐 금지', 1, NOW(), NOW()),
 (3, '가구 · 가전', 10000, '상하차 시 고객과 함께 확인',              1, NOW(), NOW());

-- 관리자 계정: 가입 화면으로는 관리자를 만들 수 없어서 직접 넣는다 (비밀번호는 BCrypt 해시로)
-- 1) EC2에서 해시 만들기 (비밀번호가 화면 · 기록에 남지 않게):
--      read -s -p '관리자 비밀번호: ' P; echo; htpasswd -bnBC 10 "" "$P" | tr -d ':\n'; echo; unset P
-- 2) 아래 ADMIN_EMAIL · ADMIN_PASSWORD_BCRYPT · 연락처를 바꾸고 주석(--)을 풀어 한 번만 실행
-- INSERT INTO member (email, password, name, phone, role, enabled, created_at, updated_at) VALUES
--  ('ADMIN_EMAIL', 'ADMIN_PASSWORD_BCRYPT', '관리자', '010-0000-0000', 'ADMIN', 1, NOW(), NOW());
