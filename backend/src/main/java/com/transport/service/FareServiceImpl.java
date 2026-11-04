package com.transport.service;

import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class FareServiceImpl implements FareService {

    // 기본 요금 정책 상수
    private static final long BASE_FARE = 5_000L;          // 기본 운임 5,000원
    private static final double BASE_DISTANCE_KM = 5.0;     // 기본 거리 5km
    private static final long FARE_PER_KM = 1_000L;         // 1km 초과당 1,000원

    private static final double BASE_WEIGHT_KG = 5.0;       // 기본 무게 5kg
    private static final long FARE_PER_KG = 500L;           // 1kg 초과당 500원

    private static final double SURCHARGE_RATE = 0.20;      // 야간/피크 할증률 20%

    @Override
    public FareCalculateResponseDTO calculateFare(FareCalculateRequestDTO requestDTO) {
        // 1. 거리 산정 (요청 DTO에 거리가 없거나 0 이하인 경우 주소 기반 더미 거리를 산정)
        // DTO 필드: originAddress, destAddress
        double distanceKm = (requestDTO.getDistanceKm() != null && requestDTO.getDistanceKm() > 0)
                ? requestDTO.getDistanceKm()
                : calculateDistanceByAddress(requestDTO.getOriginAddress(), requestDTO.getDestAddress());

        // 2. 거리 추가 요금 계산
        long distanceFare = 0L;
        if (distanceKm > BASE_DISTANCE_KM) {
            double extraDistance = distanceKm - BASE_DISTANCE_KM; // 뺄셈 기호 (-) 복구
            distanceFare = (long) (Math.ceil(extraDistance) * FARE_PER_KM);
        }

        // 3. Null-Safe 처리 포함 무게 추가 요금 계산
        // DTO 필드: cargoWeightKg
        long weightFare = 0L;
        double weight = (requestDTO.getCargoWeightKg() != null) ? requestDTO.getCargoWeightKg().doubleValue() : 0.0;
        
        if (weight > BASE_WEIGHT_KG) {
            double extraWeight = weight - BASE_WEIGHT_KG; // 뺄셈 기호 (-) 복구
            weightFare = (long) (Math.ceil(extraWeight) * FARE_PER_KG);
        }

        // 4. 할증 요금 및 최종 운임 산정
        long subTotal = BASE_FARE + distanceFare + weightFare;
        long surchargeFare = 0L;

        if (Boolean.TRUE.equals(requestDTO.getIsSurchargeTime())) {
            surchargeFare = (long) (subTotal * SURCHARGE_RATE); // 곱셈 기호 (*) 복구
        }

        long totalFare = subTotal + surchargeFare;

        log.info("Fare calculated: Distance={}km, Weight={}kg, Total={}KRW", distanceKm, weight, totalFare);

        return FareCalculateResponseDTO.builder()
                .baseFare(BASE_FARE)
                .distanceFare(distanceFare)
                .weightFare(weightFare)
                .surchargeFare(surchargeFare)
                .totalFare(totalFare)
                .calculatedDistanceKm(distanceKm)
                .build();
    }

    /**
     * 주소 기반 거리 계산 (지도 API 연동 전 임시 더미 로직)
     */
    private double calculateDistanceByAddress(String senderAddr, String receiverAddr) {
        // TODO: Kakao/Naver Map REST API 연동 예정
        return 10.0;
    }
}