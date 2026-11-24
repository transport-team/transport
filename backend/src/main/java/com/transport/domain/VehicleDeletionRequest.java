package com.transport.domain;

import java.time.LocalDateTime;
import com.transport.domain.enums.VehicleRegistrationStatus;
import com.transport.domain.enums.VehicleStatus;
import jakarta.persistence.*;
import lombok.*;

// DB에서 차량을 삭제하지 않고 비활성화하기 위한 기사 삭제 신청 이력
@Entity
@Table(name = "vehicle_deletion_request")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VehicleDeletionRequest extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_id", nullable = false)
    private Driver driver;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private VehicleRegistrationStatus status;

    @Column(length = 300)
    private String rejectionReason;

    private LocalDateTime reviewedAt;

    public void approve() {
        this.status = VehicleRegistrationStatus.APPROVED;
        this.rejectionReason = null;
        this.reviewedAt = LocalDateTime.now();
    }

    public void reject(String reason) {
        this.status = VehicleRegistrationStatus.REJECTED;
        this.rejectionReason = reason;
        this.reviewedAt = LocalDateTime.now();
    }

    public void withdraw() {
        this.status = VehicleRegistrationStatus.WITHDRAWN;
    }
}
