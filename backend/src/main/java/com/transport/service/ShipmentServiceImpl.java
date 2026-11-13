package com.transport.service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.domain.Member;
import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus; 
import com.transport.domain.enums.MemberRole;
import com.transport.dto.PageRequestDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.dto.ShipmentListResponseDTO;
import com.transport.dto.ShipmentDetailResponseDTO;
import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import com.transport.dto.ShipmentRequestDTO;
import com.transport.dto.ShipmentResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.CargoTypeRepository;
import com.transport.repository.PaymentRepository;
import com.transport.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipmentServiceImpl implements ShipmentService {

    private final FareService fareService;
    private final ShipmentRepository shipmentRepository;
    private final PaymentRepository paymentRepository;
    private final CargoTypeRepository cargoTypeRepository;
    private final MemberService memberService;
    private final VehicleTypeService vehicleTypeService;
    private final DispatchService dispatchService;
    private final StatusHistoryService statusHistoryService;

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

        Integer durationMin = fareResponse.getDurationMin();

        // 연관 엔티티 조회 (회원, 차종, 화물유형)
        Member customer = memberService.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND)); // 회원 조회 추가

        VehicleType vehicleType = vehicleTypeService.findById(requestDTO.getVehicleTypeId())
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
                .senderName(requestDTO.getSenderName())
                .senderPhone(requestDTO.getSenderPhone())
                .receiverName(requestDTO.getReceiverName())
                .receiverPhone(requestDTO.getReceiverPhone())
                .memo(requestDTO.getMemo())
                .originLat(fareResponse.getOriginLat())
                .originLng(fareResponse.getOriginLng())
                .destLat(fareResponse.getDestLat())
                .destLng(fareResponse.getDestLng())
                .distanceKm(fareResponse.getDistanceKm())
                .durationMin(durationMin)
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
                .status(shipment.getStatus().name())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .build();
    }

    @Override
    public PageResponseDTO<ShipmentListResponseDTO> getMyShipments(
            Long memberId, PageRequestDTO pageRequestDTO, ShipmentStatus status,
            LocalDate from, LocalDate to) {
        LocalDateTime fromDateTime = from == null ? null : from.atStartOfDay();
        LocalDateTime toDateTime = to == null ? null : to.plusDays(1).atStartOfDay();
        var result = shipmentRepository.findMyShipments(
                memberId, status, fromDateTime, toDateTime,
                pageRequestDTO.getPageable(org.springframework.data.domain.Sort.by("createdAt").descending()));

        return PageResponseDTO.<ShipmentListResponseDTO>withAll()
                .dtoList(result.getContent().stream().map(ShipmentListResponseDTO::from).toList())
                .pageRequestDTO(pageRequestDTO)
                .totalCount(result.getTotalElements())
                .build();
    }

    @Override
    public ShipmentDetailResponseDTO getShipmentDetail(Long shipmentId, Long memberId, MemberRole role) {
        Shipment shipment = getShipment(shipmentId);
        boolean owner = shipment.getCustomer().getId().equals(memberId);
        boolean admin = role == MemberRole.ADMIN;
        Long assignedDriverMemberId = dispatchService.getActiveDriver(shipmentId);
        boolean assignedDriver = assignedDriverMemberId != null && assignedDriverMemberId.equals(memberId);
        if (!owner && !admin && !assignedDriver) {
            throw new CustomException(ErrorCode.FORBIDDEN, "본인 배송만 조회할 수 있습니다.");
        }

        var history = statusHistoryService.getHistory(shipmentId).stream()
                .map(com.transport.dto.ShipmentStatusHistoryResponseDTO::from)
                .toList();
        return ShipmentDetailResponseDTO.from(shipment, assignedDriverMemberId, history);
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

    // 배차 서비스에서 사용할 배송 단건 조회
    @Override
    public Shipment getShipment(Long shipmentId) {
        return shipmentRepository.findById(shipmentId)
                .orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));
    }

    // 기사: 배차 대기 중인 운송 요청 목록 조회
    @Override
    public List<Shipment> getRequestedShipments() {
        return shipmentRepository.findByStatusOrderByPickupAtAsc(ShipmentStatus.REQUESTED);
    }

}