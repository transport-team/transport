package com.transport.domain;

import com.transport.domain.enums.DispatchApplicationStatus;

import jakarta.persistence.*;
import lombok.*;

// 기사의 배송 요청 지원 내역
@Entity
@Table(name = "dispatch_application")
@Getter
@ToString(exclude = {"shipment", "driver", "vehicle"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DispatchApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 지원한 배송 요청
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shipment_id", nullable = false)
    private Shipment shipment;

    // 지원한 기사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    // 지원할 때 사용할 차량
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    // 지원 상태 (PENDING, SELECTED, REJECTED, NOT_SELECTED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DispatchApplicationStatus status;

    // 물주가 지원 기사 선택
    public void select() {
        this.status = DispatchApplicationStatus.SELECTED;
    }

    // 물주가 지원 거절
    public void reject() {
        this.status = DispatchApplicationStatus.REJECTED;
    }

    // 다른 기사가 선택되어 자동 마감
    public void notSelect() {
        this.status = DispatchApplicationStatus.NOT_SELECTED;
    }
}
