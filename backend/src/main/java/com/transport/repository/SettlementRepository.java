package com.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.transport.domain.Settlement;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    // 기사별 특정 월 정산 조회
    Optional<Settlement> findByDriver_IdAndSettleMonth(
            Long driverId, String settleMonth);

    // 기사별 전체 정산 내역 (최신 월부터)
    List<Settlement> findByDriver_IdOrderBySettleMonthDesc(Long driverId);
}
