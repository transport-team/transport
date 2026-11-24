-- ADM-09 자동 월 정산 신규 테이블 (MariaDB)
-- 로컬 개발에서 spring.jpa.hibernate.ddl-auto=update이면 자동 생성될 수 있습니다.
-- 운영환경(validate)에서는 DB 백업 후 관리자가 별도 적용해야 합니다.
-- 공통 수수료율은 현재 서버 정책으로 10% 고정입니다.
-- 구버전의 driver_commission_policy 테이블이 있더라도 자동 삭제하지 않습니다.
-- 이미 확정된 settlement.commission_rate 및 지급 금액은 변경하지 않습니다.

CREATE TABLE IF NOT EXISTS settlement_period (
    settle_month VARCHAR(7) NOT NULL PRIMARY KEY,
    closed_at DATETIME(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS settlement_item (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    settlement_id BIGINT NOT NULL,
    shipment_id BIGINT NOT NULL,
    fare INT NOT NULL,
    completed_at DATETIME(6) NOT NULL,
    CONSTRAINT uk_settlement_item_shipment UNIQUE (shipment_id),
    CONSTRAINT fk_settlement_item_settlement FOREIGN KEY (settlement_id) REFERENCES settlement(id)
);
