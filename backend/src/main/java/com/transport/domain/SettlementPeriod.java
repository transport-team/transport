package com.transport.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 월 마감 기록: 한 번 등록되면 재집계하지 않는다.
@Entity
@Table(name = "settlement_period")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class SettlementPeriod {
    @Id
    @Column(name = "settle_month", length = 7)
    private String settleMonth;

    @Column(nullable = false)
    private LocalDateTime closedAt;
}
