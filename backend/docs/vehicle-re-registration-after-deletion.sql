-- MariaDB/MySQL: 차량 삭제 승인(INACTIVE) 이후 같은 차종/차량번호 재등록 허용
-- 각 개발자의 로컬 DB에 한 번만 실행합니다. DB 백업 후 적용하세요.
-- Hibernate ddl-auto=update는 기존 UNIQUE 인덱스를 자동으로 제거하지 않습니다.
-- 실행 전 USE <사용중인_DB명>; 또는 올바른 스키마를 선택하세요.
--
-- 기존 vehicle 행/차량번호/기사 ID와 배차 및 삭제 승인 이력은 보존합니다.
-- 사용 중인 차량(AVAILABLE/MAINTENANCE) 간 차종·번호 중복은 계속 DB에서 막습니다.
-- 아래 생성 칼럼은 비활성(INACTIVE) 행에서는 NULL이므로 UNIQUE 제한 대상에서 제외됩니다.

ALTER TABLE `vehicle`
    ADD COLUMN `active_driver_id` BIGINT
        GENERATED ALWAYS AS (IF(`status` = 'INACTIVE', NULL, `driver_id`)) STORED,
    ADD COLUMN `active_vehicle_type_id` BIGINT
        GENERATED ALWAYS AS (IF(`status` = 'INACTIVE', NULL, `vehicle_type_id`)) STORED,
    ADD COLUMN `active_plate_no` VARCHAR(20)
        GENERATED ALWAYS AS (IF(`status` = 'INACTIVE', NULL, `plate_no`)) STORED,
    ADD UNIQUE INDEX `uk_vehicle_active_driver_type` (`active_driver_id`, `active_vehicle_type_id`),
    ADD UNIQUE INDEX `uk_vehicle_active_plate_no` (`active_plate_no`),
    -- 기존 UNIQUE 인덱스가 외래키용으로 사용 중일 수 있어 일반 인덱스도 따로 확보합니다.
    ADD INDEX `idx_vehicle_driver_history` (`driver_id`),
    ADD INDEX `idx_vehicle_type_history` (`vehicle_type_id`);

-- 이전 버전에서 생성한 driver_id + vehicle_type_id UNIQUE 인덱스를 찾아 제거합니다.
-- 실제 인덱스 이름이 달라도 컬럼 조합으로 찾습니다.
SET @old_driver_type_unique = (
    SELECT MIN(`index_name`) FROM (
        SELECT `index_name`
          FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = 'vehicle' AND non_unique = 0
         GROUP BY `index_name`
        HAVING COUNT(*) = 2
           AND SUM(`column_name` = 'driver_id') = 1
           AND SUM(`column_name` = 'vehicle_type_id') = 1
    ) AS `old_driver_type_indexes`
);
SET @ddl = IF(@old_driver_type_unique IS NULL,
              'SELECT ''old driver/type unique index not found''',
              CONCAT('ALTER TABLE `vehicle` DROP INDEX `', REPLACE(@old_driver_type_unique, '`', '``'), '`'));
PREPARE old_index_stmt FROM @ddl;
EXECUTE old_index_stmt;
DEALLOCATE PREPARE old_index_stmt;

-- 과거 @Column(unique=true)로 생성한 plate_no UNIQUE 인덱스 제거.
-- Hibernate가 임의 이름(UK_...)으로 만든 경우에도 대응합니다.
SET @old_plate_unique = (
    SELECT MIN(`index_name`) FROM (
        SELECT `index_name`
          FROM information_schema.statistics
         WHERE table_schema = DATABASE() AND table_name = 'vehicle' AND non_unique = 0
         GROUP BY `index_name`
        HAVING COUNT(*) = 1
           AND SUM(`column_name` = 'plate_no') = 1
    ) AS `old_plate_indexes`
);
SET @ddl = IF(@old_plate_unique IS NULL,
              'SELECT ''old plate unique index not found''',
              CONCAT('ALTER TABLE `vehicle` DROP INDEX `', REPLACE(@old_plate_unique, '`', '``'), '`'));
PREPARE old_index_stmt FROM @ddl;
EXECUTE old_index_stmt;
DEALLOCATE PREPARE old_index_stmt;

-- 점검: 원래 2개 UNIQUE 대신 새 활성 차량 전용 인덱스 2개가 있어야 합니다.
SHOW INDEX FROM `vehicle`;
