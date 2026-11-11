# ERD — 테이블 21개

- 관리자: 팀원 2 (테이블 변경은 팀원 2에게 요청 → 주간 회의에서 확정 → 이 문서 수정)
- 공통: 모든 테이블은 `id BIGINT PK AUTO_INCREMENT`, `created_at`, `updated_at`(BaseEntity)
- enum은 `@Enumerated(EnumType.STRING)`, 연관관계는 `@ManyToOne(fetch = LAZY)` 단방향 기본

## 주요 관계

| 관계 | 설명 |
|---|---|
| member 1 : 0..1 driver | 기사 회원만 driver 정보를 가짐 |
| member 1 : N shipment | 고객이 신청한 배송 |
| member 1 : N term_agreement N : 1 term | 약관 동의 이력 |
| member 1 : 0..1 customer_address | 고객 기본 배송지 |
| member 1 : N inquiry / notice / notification | 문의 작성자 / 공지 작성 관리자 / 알림 수신자 |
| driver 1 : N vehicle | 기사 1명이 여러 차량 등록 가능 (같은 차종은 1대만) |
| driver 1 : N driver_schedule / dispatch / dispatch_application / settlement / review | 근무 일정 / 확정 배차 / 배차 신청 / 정산 / 받은 리뷰 |
| shipment 1 : N dispatch_application | 배송 1건에 여러 기사가 지원 가능 |
| vehicle 1 : N dispatch_application | 차량 1대로 여러 배송에 지원한 이력 보존 |
| vehicle_type 1 : N vehicle / shipment | 차종 (shipment는 요청 차종) |
| cargo_type 1 : N shipment | 화물 종류 |
| shipment 1 : 1 payment | 배송당 결제 1건 |
| shipment 1 : N dispatch | 재배차 이력 (active는 1건) |
| shipment 1 : N shipment_status_history / shipment_image / location_log | 이력 / 사진 / 위치 |
| shipment 1 : 0..1 review | 배송당 리뷰 1개 |

배송과 기사는 직접 연결하지 않고 dispatch를 거친다.

## 팀원 3 — 배송 · 운임 · 결제

### shipment (배송, 모두가 참조하는 중심 테이블)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| customer_id (FK) | BIGINT | 신청 고객 → member |
| vehicle_type_id (FK) | BIGINT | 요청 차종 (무게로 자동 추천) |
| cargo_type_id (FK) | BIGINT | 화물 종류 |
| origin_address / origin_detail | VARCHAR | 출발지 주소 / 상세 |
| origin_lat / origin_lng | DOUBLE | 출발지 위도 / 경도 |
| dest_address / dest_detail | VARCHAR | 도착지 주소 / 상세 |
| dest_lat / dest_lng | DOUBLE | 도착지 위도 / 경도 |
| distance_km / duration_min | DOUBLE / INT | 도로 거리 / 예상 소요(분) |
| cargo_name / cargo_weight_kg | VARCHAR / INT | 화물명 / 무게 |
| pickup_at | DATETIME | 희망 상차 일시 (할증 기준) |
| sender_name / sender_phone | VARCHAR | 보내는 사람 |
| receiver_name / receiver_phone | VARCHAR | 수령인 |
| memo / cancel_reason | VARCHAR(500) | 요청사항 / 취소 사유 |
| fare | INT | 운임 = 결제 금액 (서버 계산) |
| status | VARCHAR(20) | 배송 상태 (status.md) |
| version | BIGINT | @Version 낙관적 락 |

### cargo_type (화물 종류)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| name (UQ) | VARCHAR(30) | 일반 · 파손주의 · 가구/가전 |
| extra_fee | INT | 추가요금(원) |
| handling_note | VARCHAR(255) | 기사에게 보여줄 취급 안내 |
| active | BOOLEAN | 사용 여부 |

### payment (결제, shipment와 1:1)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| shipment_id (FK, UQ) | BIGINT | 배송 |
| order_id (UQ) | VARCHAR(64) | 주문번호 (재시도 시 새로 발급) |
| payment_key | VARCHAR(200) | 토스 결제 키 (승인 후) |
| amount / method | INT / VARCHAR | 결제 금액(승인 전 검증) / 수단 |
| status | VARCHAR(20) | READY · PAID · CANCELED · EXPIRED |
| paid_at / canceled_at / cancel_reason | DATETIME / DATETIME / VARCHAR | 승인·취소 정보 |

## 팀원 1 — 공통 · 인증 · 운영

### member
| 컬럼 | 타입 | 설명 |
|---|---|---|
| email (UQ) | VARCHAR(100) | 로그인 아이디 |
| password | VARCHAR(255) | BCrypt |
| name / phone | VARCHAR | 이름 / 연락처 |
| role | VARCHAR(20) | CUSTOMER · DRIVER · ADMIN |
| enabled | BOOLEAN | 활성 여부 |

### term
| 컬럼 | 타입 | 설명 |
|---|---|---|
| title / content | VARCHAR(100) / TEXT | 약관 이름 / 내용 |
| required | BOOLEAN | 필수 동의 여부 |
| version | VARCHAR(20) | 예: v1.0 |
| active | BOOLEAN | 사용 여부 |

### term_agreement (member + term UQ)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| member_id (FK) | BIGINT | 회원 |
| term_id (FK) | BIGINT | 약관 |
| agreed_at | DATETIME | 동의 시각 |

### customer_address (고객 기본 배송지)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| member_id (FK, UQ) | BIGINT | 고객 회원 |
| label | VARCHAR(50) | 배송지 이름 |
| origin_address / origin_detail | VARCHAR / VARCHAR | 출발지 주소 / 상세 |
| dest_address / dest_detail | VARCHAR / VARCHAR | 도착지 주소 / 상세 |

### notice
| 컬럼 | 타입 | 설명 |
|---|---|---|
| title / content | VARCHAR / TEXT | 제목 / 내용 |
| writer_id (FK) | BIGINT | 작성 관리자 |
| view_count | INT | 조회수 |

### faq
| 컬럼 | 타입 | 설명 |
|---|---|---|
| category | VARCHAR(30) | 배송 · 결제 · 회원 |
| question / answer | VARCHAR(255) / TEXT | 질문 / 답변 |
| sort_order / active | INT / BOOLEAN | 정렬 / 노출 |

### inquiry
| 컬럼 | 타입 | 설명 |
|---|---|---|
| member_id (FK) | BIGINT | 작성 회원 |
| title / content | VARCHAR / TEXT | 제목 / 내용 |
| answer / answered_at | TEXT / DATETIME | 답변 / 답변일 |
| status | VARCHAR(20) | WAITING · ANSWERED |

## 팀원 2 — 기사 · 차량 · 배차 · 정산

### driver (member와 1:1)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| member_id (FK, UQ) | BIGINT | 기사 회원 |
| license_type / career_years | VARCHAR(20) / INT | 면허 종류 / 경력(년) |
| status | VARCHAR(20) | PENDING · ACTIVE · INACTIVE |
| approved_at | DATETIME | 승인 일시 |

### vehicle_type (차종 · 요금 규칙)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| code (UQ) / name | VARCHAR | SMALL · TON1 · TON2_5 · TON5 / 표시 이름 |
| max_load_kg | INT | 최대 적재량 |
| base_fare / base_distance_km | INT | 기본요금 / 기본 포함 거리 |
| per_km_fare | INT | 초과 1km당 |
| night_rate / weekend_rate / heavy_rate | INT | 할증률(%) |
| active | BOOLEAN | 사용 여부 |

### vehicle
| 컬럼 | 타입 | 설명 |
|---|---|---|
| plate_no (UQ) | VARCHAR(20) | 차량번호 |
| vehicle_type_id (FK) | BIGINT | 차종 |
| status | VARCHAR(20) | AVAILABLE · MAINTENANCE · INACTIVE |
| driver_id (FK, NULL) | BIGINT | 차량 소유 기사 (미배정 시 NULL) |

- 제약조건: `UNIQUE(driver_id, vehicle_type_id)` — 기사별 동일 차종 중복 등록 방지
- 차량번호 `plate_no`는 전체 차량에서 UNIQUE

### driver_schedule (driver + work_date UQ)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| driver_id (FK) | BIGINT | 기사 |
| work_date | DATE | 날짜 |
| start_time / end_time | TIME | 근무 가능 시간 |
| status | VARCHAR(20) | AVAILABLE · OFF |

### dispatch (재배차 이력 보존)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| shipment_id (FK) | BIGINT | 배송 |
| driver_id / vehicle_id (FK) | BIGINT | 배정 기사 / 차량 |
| assigned_by (FK) | BIGINT | 배차를 확정한 회원 (고객 선택 시 해당 고객) |
| active | BOOLEAN | 현재 유효 배차 (배송당 1건) |
| assigned_at / released_at | DATETIME | 배정 / 해제 |

### dispatch_application (기사 배차 지원 내역)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id (PK) | BIGINT | 배차 신청 ID |
| shipment_id (FK) | BIGINT | 지원한 배송 요청 |
| driver_id (FK) | BIGINT | 지원한 기사 |
| vehicle_id (FK) | BIGINT | 지원 시 사용할 차량 |
| status | VARCHAR(20) | PENDING · SELECTED · REJECTED · NOT_SELECTED |

- 배송 1건에 여러 기사가 지원할 수 있음
- 기사가 지원하면 `PENDING`
- 고객이 선택한 지원은 `SELECTED`
- 고객이 개별 거절한 지원은 `REJECTED`
- 다른 기사가 선택되어 자동 마감된 지원은 `NOT_SELECTED`
- 확정된 배차 정보는 별도의 `dispatch` 테이블에 저장

### settlement (driver + settle_month UQ)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| driver_id (FK) | BIGINT | 기사 |
| settle_month | CHAR(7) | 예: 2026-12 |
| completed_count | INT | 완료 건수 |
| total_fare / payout_amount | INT | 운임 합계 / 지급액 |
| commission_rate | INT | 수수료율(%) |
| status / paid_at | VARCHAR / DATETIME | PENDING · PAID / 지급일 |

## 팀원 4 — 진행 · 실시간 · 리뷰

### shipment_status_history
| 컬럼 | 타입 | 설명 |
|---|---|---|
| shipment_id (FK) | BIGINT | 배송 |
| from_status / to_status | VARCHAR(20) | 이전 / 다음 |
| changed_by (FK, NULL) | BIGINT | 변경자 (시스템은 NULL) |
| memo / changed_at | VARCHAR / DATETIME | 사유 / 시각 |

### shipment_image
| 컬럼 | 타입 | 설명 |
|---|---|---|
| shipment_id (FK) | BIGINT | 배송 |
| type | VARCHAR(20) | PICKUP · RECEIPT · ETC |
| file_name | VARCHAR(255) | UUID_원본명 |
| uploaded_by (FK) | BIGINT | 올린 기사 |

### location_log (shipment_id + recorded_at 인덱스, 30일 후 삭제)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| shipment_id (FK) | BIGINT | 배송 |
| driver_id (FK) | BIGINT | 기사 |
| lat / lng | DOUBLE | 위도 / 경도 |
| recorded_at | DATETIME | 측정 시각 |

### review (shipment UQ)
| 컬럼 | 타입 | 설명 |
|---|---|---|
| shipment_id (FK, UQ) | BIGINT | 배송 |
| customer_id (FK) | BIGINT | 작성 고객 |
| driver_id (FK) | BIGINT | 평가 대상 기사 |
| rating / content | TINYINT / VARCHAR(500) | 1~5 / 후기 |

### notification
| 컬럼 | 타입 | 설명 |
|---|---|---|
| receiver_id (FK) | BIGINT | 받는 회원 |
| shipment_id (FK, NULL) | BIGINT | 관련 배송 |
| type | VARCHAR(30) | DISPATCHED · STATUS_CHANGED · INQUIRY_ANSWERED · REVIEW_REQUEST |
| message / is_read | VARCHAR(255) / BOOLEAN | 문구 / 읽음 |

## 설계 메모
- 기사 평균 평점은 review에서 AVG로 계산 (느려지면 driver에 평균 컬럼 추가)
- Refresh 토큰은 서버에 저장하지 않는 stateless 구조로 시작
- 시드 데이터는 `backend/src/main/resources/data.sql` (컬럼 이름이 바뀌면 함께 수정)
