-- 기사 본인 입력 보유 자격증명 (검증 완료 자격과 혼동하지 않도록 관리자 UI에 별도 표시)
-- 기존 driver 레코드 값은 NULL로 유지됩니다.
-- 운영 DB에서는 백업 및 컬럼 존재 여부 확인 후 실행하세요.
ALTER TABLE driver ADD COLUMN certifications VARCHAR(200) NULL;
