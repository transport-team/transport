package com.transport.domain;

import java.time.LocalDateTime;

import com.transport.domain.enums.VehicleRegistrationStatus;

import jakarta.persistence.*;
import lombok.*;

// 기사의 차량 등록 신청 정보
@Entity
@Table(name = "vehicle_registration_request")
@Getter
@ToString(exclude = { "driver", "vehicleType" })
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleRegistrationRequest extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 차량 등록을 신청한 기사
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    // 신청한 차량의 차종
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", nullable = false)
    private VehicleType vehicleType;

    // 신청한 차량번호
    @Column(nullable = false, length = 20)
    private String plateNo;

    // 신청 상태 (PENDING, APPROVED, REJECTED)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleRegistrationStatus status;

    // 관리자가 거절한 사유
    @Column(length = 300)
    private String rejectionReason;

    // 관리자가 승인 또는 거절한 시각
    private LocalDateTime reviewedAt;
    
    // 차량 등록 신청 승인
    public void approve() {
        this.status = VehicleRegistrationStatus.APPROVED;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
    }

    // 차량 등록 신청 거절
    public void reject(String rejectionReason) {
        this.status = VehicleRegistrationStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.reviewedAt = LocalDateTime.now();
    }

}