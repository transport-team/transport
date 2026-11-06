package com.transport.service;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.domain.Member; // 추가
import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus; 
import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.CargoTypeRepository;
import com.transport.repository.PaymentRepository;
import com.transport.repository.ShipmentRepository;
import com.transport.repository.VehicleTypeRepository;
import com.transport.repository.MemberRepository; // 추가

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipmentServiceImpl implements ShipmentService {

    private final FareService fareService;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final CargoTypeRepository cargoTypeRepository;
    private final MemberRepository memberRepository; // 회원 조회를 위해 추가

    @Override
    @Transactional
    public ShipmentResponseDTO createShipment(Long memberId, ShipmentRequestDTO requestDTO) {
        
        // 1. 운임 계산 (누락되었던 cargoTypeId, pickupAt 추가)
        FareCalculateRequestDTO fareRequest = FareCalculateRequestDTO.builder()
                .originAddress(requestDTO.getOriginAddress())
                .destAddress(requestDTO.getDestAddress())
                .vehicleTypeId(requestDTO.getVehicleTypeId())
                .cargoTypeId(requestDTO.getCargoTypeId()) // 필수 추가
                .cargoWeightKg(requestDTO.getCargoWeightKg())
                .pickupAt(requestDTO.getPickupAt())       // 필수 추가
                .build();

        FareCalculateResponseDTO fareResponse = fareService.calculateFare(fareRequest);

        // 연관 엔티티 조회 (회원, 차종, 화물유형)
        Member customer = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND)); // 회원 조회 추가

        VehicleType vehicleType = vehicleTypeRepository.findById(requestDTO.getVehicleTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        
        CargoType cargoType = cargoTypeRepository.findById(requestDTO.getCargoTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 2. 배송 엔티티 생성
        Shipment shipment = Shipment.builder()
                .customer(customer) // DB NOT NULL 에러 방지를 위해 고객 매핑 추가
                .vehicleType(vehicleType)
                .cargoType(cargoType)
                .originAddress(requestDTO.getOriginAddress())
                .originDetail(requestDTO.getOriginDetail())
                .destAddress(requestDTO.getDestAddress())
                .destDetail(requestDTO.getDestDetail())
                .cargoName(requestDTO.getCargoName())
                .cargoWeightKg(requestDTO.getCargoWeightKg())
                .pickupAt(requestDTO.getPickupAt())
                .receiverName(requestDTO.getReceiverName())
                .receiverPhone(requestDTO.getReceiverPhone())
                .memo(requestDTO.getMemo())
                .distanceKm(fareResponse.getDistanceKm()) 
                .fare(fareResponse.getTotalFare())      
                .status(ShipmentStatus.PENDING_PAYMENT)         
                .build();

        shipmentRepository.save(shipment);

        // 3. 주문번호 생성 (UUID 기반으로 고유 식별자 생성)
        String orderId = UUID.randomUUID().toString();

        // 4. 결제 엔티티 생성
        Payment payment = Payment.builder()
                .shipment(shipment)
                .orderId(orderId)
                .amount(fareResponse.getTotalFare())
                .status(PaymentStatus.READY) // 초기 상태는 결제 대기
                .build();

        paymentRepository.save(payment);

        // 5. 프론트엔드 결제 연동용 DTO 반환
        return ShipmentResponseDTO.builder()
                .shipmentId(shipment.getId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .build();
    }

    // 관리자 대시보드: 지금 상태별 배송 건수 (팀원 1 대시보드가 쓰는 읽기 전용 메서드)
    @Override
    public Map<ShipmentStatus, Long> countByStatus() {
        Map<ShipmentStatus, Long> counts = new EnumMap<>(ShipmentStatus.class);
        for (ShipmentStatus status : ShipmentStatus.values()) {
            counts.put(status, 0L);  // 배송이 없는 상태도 0으로
        }
        shipmentRepository.countGroupByStatus()
                .forEach(row -> counts.put(row.getStatus(), row.getShipmentCount()));
        return counts;
    }
}