package com.transport.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus; // 본인 프로젝트의 실제 Enum 사용
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

    @Override
    @Transactional
    public ShipmentResponseDTO createShipment(ShipmentRequestDTO requestDTO) {
        
        // 1. 운임 계산
        // 기존 FareService를 재사용하기 위해 ShipmentRequestDTO -> FareCalculateRequestDTO 매핑
        FareCalculateRequestDTO fareRequest = FareCalculateRequestDTO.builder()
                .originAddress(requestDTO.getOriginAddress())
                .destAddress(requestDTO.getDestAddress())
                .vehicleTypeId(requestDTO.getVehicleTypeId())
                .cargoWeightKg(requestDTO.getCargoWeightKg())
                .build();

        FareCalculateResponseDTO fareResponse = fareService.calculateFare(fareRequest);

        // 연관 엔티티 조회 (차종, 화물유형)
        VehicleType vehicleType = vehicleTypeRepository.findById(requestDTO.getVehicleTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        CargoType cargoType = cargoTypeRepository.findById(requestDTO.getCargoTypeId())
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));

        // 2. 배송 엔티티 생성
        Shipment shipment = Shipment.builder()
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
                // 계산된 결과값 매핑
                .distanceKm(fareResponse.getDistanceKm()) // 엔티티 필드명(distance)에 맞게 수정 필요
                .fare(fareResponse.getTotalFare())      // 엔티티 필드명(fare)에 맞게 수정 필요
                .status(ShipmentStatus.PENDING_PAYMENT)         // 배송 상태 Enum (결제대기 또는 접수완료)
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
}