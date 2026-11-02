-- 시드 데이터 (공통 파일 — 관리: 팀원 2)
-- 실행 조건: application-local.yml에서 spring.sql.init.mode: always
--           + 아래 테이블의 엔티티가 모두 만들어진 뒤 (없는 테이블이 있으면 서버가 시작되지 않음)
-- INSERT IGNORE라 서버를 다시 켜도 중복으로 들어가지 않는다.
-- 컬럼 이름은 docs/erd.md 기준 (JPA가 camelCase → snake_case로 바꾼다). 엔티티와 다르면 이 파일을 맞춘다.
-- 테스트 계정 비밀번호는 모두 1234 (BCrypt)

-- ===== 팀원 1: 회원 · 약관 · 공지 · FAQ =====
INSERT IGNORE INTO member (id, email, password, name, phone, role, enabled, created_at, updated_at) VALUES
 (1, 'admin@transport.test',    '$2a$10$x2H.ZSz9k/3pFeUR0AZEEeaYDQVdbzitmyFkBdofCGmBiXAwmnL..', '관리자',   '010-0000-0001', 'ADMIN',    1, NOW(), NOW()),
 (2, 'customer@transport.test', '$2a$10$x2H.ZSz9k/3pFeUR0AZEEeaYDQVdbzitmyFkBdofCGmBiXAwmnL..', '테스트고객', '010-0000-0002', 'CUSTOMER', 1, NOW(), NOW()),
 (3, 'driver@transport.test',   '$2a$10$x2H.ZSz9k/3pFeUR0AZEEeaYDQVdbzitmyFkBdofCGmBiXAwmnL..', '테스트기사', '010-0000-0003', 'DRIVER',   1, NOW(), NOW());

INSERT IGNORE INTO term (id, title, content, required, version, active, created_at, updated_at) VALUES
 (1, '서비스 이용약관',        '서비스 이용약관 내용 (작성 예정)',        1, 'v1.0', 1, NOW(), NOW()),
 (2, '개인정보 수집 · 이용 동의', '개인정보 수집 · 이용 동의 내용 (작성 예정)', 1, 'v1.0', 1, NOW(), NOW()),
 (3, '마케팅 정보 수신 동의',    '마케팅 정보 수신 동의 내용 (작성 예정)',    0, 'v1.0', 1, NOW(), NOW());

INSERT IGNORE INTO notice (id, title, content, writer_id, view_count, created_at, updated_at) VALUES
 (1, '서비스 오픈 안내', '운송서비스 운용 사이트가 열렸습니다.', 1, 0, NOW(), NOW());

INSERT IGNORE INTO faq (id, category, question, answer, sort_order, active, created_at, updated_at) VALUES
 (1, '결제', '결제하지 않으면 어떻게 되나요?', '신청 후 30분 안에 결제하지 않으면 자동으로 취소됩니다.', 1, 1, NOW(), NOW()),
 (2, '배송', '배송 중 기사 위치를 볼 수 있나요?', '운송중 상태일 때 배송 상세 화면 지도에서 볼 수 있습니다.', 2, 1, NOW(), NOW());

-- ===== 팀원 2: 차종 · 기사 · 차량 · 근무 일정 =====
INSERT IGNORE INTO vehicle_type (id, code, name, max_load_kg, base_fare, base_distance_km, per_km_fare, night_rate, weekend_rate, heavy_rate, active, created_at, updated_at) VALUES
 (1, 'SMALL',  '소형 (다마스 · 라보)', 500,   30000, 10, 1000, 20, 10, 10, 1, NOW(), NOW()),
 (2, 'TON1',   '1톤',               1000,  50000, 10, 1500, 20, 10, 10, 1, NOW(), NOW()),
 (3, 'TON2_5', '2.5톤',             2500,  90000, 10, 2200, 20, 10, 10, 1, NOW(), NOW()),
 (4, 'TON5',   '5톤',               5000, 150000, 10, 3000, 20, 10, 10, 1, NOW(), NOW());

INSERT IGNORE INTO driver (id, member_id, license_type, career_years, status, approved_at, created_at, updated_at) VALUES
 (1, 3, '1종 보통', 5, 'ACTIVE', NOW(), NOW(), NOW());

INSERT IGNORE INTO vehicle (id, plate_no, vehicle_type_id, status, driver_id, created_at, updated_at) VALUES
 (1, '12가3456', 2, 'AVAILABLE', 1, NOW(), NOW());

-- 테스트 기사의 근무 일정: 오늘부터 7일간 08:00 ~ 20:00
INSERT IGNORE INTO driver_schedule (driver_id, work_date, start_time, end_time, status, created_at, updated_at) VALUES
 (1, CURDATE(),                     '08:00:00', '20:00:00', 'AVAILABLE', NOW(), NOW()),
 (1, CURDATE() + INTERVAL 1 DAY,    '08:00:00', '20:00:00', 'AVAILABLE', NOW(), NOW()),
 (1, CURDATE() + INTERVAL 2 DAY,    '08:00:00', '20:00:00', 'AVAILABLE', NOW(), NOW()),
 (1, CURDATE() + INTERVAL 3 DAY,    '08:00:00', '20:00:00', 'AVAILABLE', NOW(), NOW()),
 (1, CURDATE() + INTERVAL 4 DAY,    '08:00:00', '20:00:00', 'AVAILABLE', NOW(), NOW()),
 (1, CURDATE() + INTERVAL 5 DAY,    '08:00:00', '20:00:00', 'AVAILABLE', NOW(), NOW()),
 (1, CURDATE() + INTERVAL 6 DAY,    '08:00:00', '20:00:00', 'AVAILABLE', NOW(), NOW());

-- ===== 팀원 3: 화물 종류 =====
INSERT IGNORE INTO cargo_type (id, name, extra_fee, handling_note, active, created_at, updated_at) VALUES
 (1, '일반',       0,     NULL,                                   1, NOW(), NOW()),
 (2, '파손주의',   5000,  '완충재 포장 확인, 적재 시 위에 다른 짐 금지', 1, NOW(), NOW()),
 (3, '가구 · 가전', 10000, '상하차 시 고객과 함께 확인',              1, NOW(), NOW());
