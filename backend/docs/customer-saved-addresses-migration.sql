-- 고객 주소록: 기존 customer_address(출발·도착 세트)는 유지하고 별도 테이블을 추가합니다.
-- MySQL 기준. 배포 환경에서 Hibernate ddl-auto=update가 아닌 경우에만 운영 DB 변경 절차에 따라 실행하세요.
CREATE TABLE IF NOT EXISTS customer_saved_address (
    id BIGINT NOT NULL AUTO_INCREMENT,
    member_id BIGINT NOT NULL,
    label VARCHAR(50) NOT NULL,
    address VARCHAR(500) NOT NULL,
    detail VARCHAR(200) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_customer_saved_address_member (member_id),
    CONSTRAINT fk_customer_saved_address_member FOREIGN KEY (member_id) REFERENCES member (id)
);

-- 기존 customer_address 데이터는 삭제하거나 자동 변환하지 않습니다.
-- 기존 출발지·도착지 세트를 각각 새 주소록으로 옮길지 여부는 팀에서 정책을 정한 뒤 처리하세요.
