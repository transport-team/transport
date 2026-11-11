package com.transport.domain;

import java.time.LocalDateTime;

import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import jakarta.persistence.*;
import lombok.*;

// 배송 (모두가 참조하는 중심 테이블). 기사는 dispatch를 거쳐 연결한다
@Entity
@Table(name = "shipment")
@Getter
@ToString(exclude = {"customer", "vehicleType", "cargoType"})
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Shipment extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "customer_id", nullable = false)
	private Member customer;  // 신청 고객

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_type_id", nullable = false)
	private VehicleType vehicleType;  // 요청 차종

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "cargo_type_id", nullable = false)
	private CargoType cargoType;

	@Column(nullable = false)
	private String originAddress;

	private String originDetail;

	private double originLat;

	private double originLng;

	@Column(nullable = false)
	private String destAddress;

	private String destDetail;

	private double destLat;

	private double destLng;

	private double distanceKm;  // 도로 거리

	private int durationMin;  // 예상 소요(분)

	@Column(nullable = false)
	private String cargoName;

	private int cargoWeightKg;

	@Column(nullable = false)
	private LocalDateTime pickupAt;  // 희망 상차 일시 (할증 기준)

	@Column(nullable = false)
	private String receiverName;

	@Column(nullable = false)
	private String receiverPhone;

	@Column(length = 500)
	private String memo;  // 요청사항

	@Column(length = 500)
	private String cancelReason;

	private int fare;  // 운임 = 결제 금액 (서버 FareCalculator로만 계산)

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, columnDefinition = "VARCHAR(20)")
	private ShipmentStatus status;

	@Version
	private Long version;  // 낙관적 락

	public void updateStatus(ShipmentStatus status) {
		this.status = status;
	}

    // 배송 상태 전이 규칙을 검사한 후 상태 변경
    public void changeStatus(ShipmentStatus next) {
        if (this.status == null || !this.status.canChangeTo(next)) {
            throw new CustomException(ErrorCode.INVALID_STATUS_CHANGE);
        }

        this.status = next;
    }
	
}
