package com.transport.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 마감 시점 운송별 고정 내역. shipment_id UNIQUE로 중복 정산을 방지한다.
@Entity
@Table(name = "settlement_item", uniqueConstraints =
        @UniqueConstraint(name = "uk_settlement_item_shipment", columnNames = "shipment_id"))
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SettlementItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @Column(name = "shipment_id", nullable = false)
    private Long shipmentId;

    @Column(nullable = false)
    private int fare;

    @Column(nullable = false)
    private LocalDateTime completedAt;
}
