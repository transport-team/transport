-- ADM-09 관리자 지급 확인 감사 항목 (MySQL)
-- application-prod.yml은 ddl-auto: validate이므로 새 코드 배포 전에 DBA가 적용해야 한다.
-- 이미 같은 컬럼이 존재하면 실행하지 않는다. 운영 DB 백업 후 실행 권장.
ALTER TABLE settlement
    ADD COLUMN paid_by_admin_email VARCHAR(100) NULL,
    ADD COLUMN transfer_reference VARCHAR(100) NULL;
-- 기존 PAID 행은 새 증빙 정보가 없어 null로 남는다. 이전 기록은 임의로 채우지 않는다.
