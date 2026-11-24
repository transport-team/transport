package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.CargoType;
import com.transport.domain.VehicleType;
import com.transport.dto.FareCalculateRequestDTO;
import com.transport.dto.FareCalculateResponseDTO;
import com.transport.repository.CargoTypeRepository;
import com.transport.service.DistanceService.Route;

@ExtendWith(MockitoExtension.class)
class FareServiceTest {

    @Mock
    private VehicleTypeService vehicleTypeService;

    @Mock
    private CargoTypeRepository cargoTypeRepository;

    @Mock
    private DistanceService distanceService;

    @Spy
    private FareCalculator fareCalculator = new FareCalculator();

    private FareServiceImpl fareService;

    private VehicleType smallVehicle;
    private VehicleType midVehicle;
    private CargoType normalCargo;
    private CargoType fragileCargo;

    @BeforeEach
    void setUp() {
        fareService = new FareServiceImpl(
                vehicleTypeService,
                cargoTypeRepository,
                distanceService,
                fareCalculator
        );

        smallVehicle = VehicleType.builder()
                .id(1L)
                .name("소형(다마스·라보)")
                .baseFare(30000)
                .baseDistanceKm(5)
                .perKmFare(1000)
                .maxLoadKg(500)
                .nightRate(20)
                .weekendRate(10)
                .heavyRate(15)
                .active(true)
                .build();

        midVehicle = VehicleType.builder()
                .id(2L)
                .name("1톤")
                .baseFare(45000)
                .baseDistanceKm(10)
                .perKmFare(1500)
                .maxLoadKg(1000)
                .nightRate(20)
                .weekendRate(10)
                .heavyRate(15)
                .active(true)
                .build();

        normalCargo = CargoType.builder()
                .id(1L)
                .name("일반")
                .extraFee(0)
                .active(true)
                .build();

        fragileCargo = CargoType.builder()
                .id(2L)
                .name("파손주의/가전")
                .extraFee(5000)
                .active(true)
                .build();
    }

    @Test
    @DisplayName("예시 1: 평일 주간 기본거리 이내 · 추가요금 없음")
    void calculateFare_example1_basicNoSurcharge() {
        LocalDateTime pickupAt = LocalDateTime.of(2026, 11, 24, 14, 0); // 화요일 14시
        FareCalculateRequestDTO req = FareCalculateRequestDTO.builder()
                .originAddress("강원특별자치도 춘천시 한림대학길 1")
                .destAddress("강원특별자치도 춘천시 시청길 11")
                .vehicleTypeId(1L)
                .cargoTypeId(1L)
                .cargoWeightKg(100)
                .pickupAt(pickupAt)
                .build();

        when(vehicleTypeService.findById(1L)).thenReturn(Optional.of(smallVehicle));
        when(cargoTypeRepository.findById(1L)).thenReturn(Optional.of(normalCargo));
        when(distanceService.findRoute(req.getOriginAddress(), req.getDestAddress()))
                .thenReturn(new Route(4.2, 15, 37.886, 127.738, 37.881, 127.729));
        when(vehicleTypeService.findAllEntities()).thenReturn(List.of(smallVehicle, midVehicle));

        FareCalculateResponseDTO res = fareService.calculateFare(req);

        assertThat(res.getBaseFare()).isEqualTo(30000);
        assertThat(res.getDistanceFare()).isEqualTo(0);
        assertThat(res.getCargoFare()).isEqualTo(0);
        assertThat(res.getSurchargeFare()).isEqualTo(0);
        assertThat(res.getTotalFare()).isEqualTo(30000);
        assertThat(res.getRecommendedVehicleTypeId()).isEqualTo(1L);
        assertThat(res.isInquiryRequired()).isFalse();
    }

    @Test
    @DisplayName("예시 2: 거리 초과(올림 적용) + 화물 추가요금 반영")
    void calculateFare_example2_distanceOverAndCargoFee() {
        LocalDateTime pickupAt = LocalDateTime.of(2026, 11, 25, 10, 0); // 수요일 10시
        FareCalculateRequestDTO req = FareCalculateRequestDTO.builder()
                .originAddress("서울특별시 종로구 세종대로 110")
                .destAddress("서울특별시 강남구 테헤란로 152")
                .vehicleTypeId(1L)
                .cargoTypeId(2L)
                .cargoWeightKg(300)
                .pickupAt(pickupAt)
                .build();

        // ceil(12.3) = 13 -> 초과 8km * 1000원 = 8000원, 화물 추가 5000원 -> 합계 43000원
        when(vehicleTypeService.findById(1L)).thenReturn(Optional.of(smallVehicle));
        when(cargoTypeRepository.findById(2L)).thenReturn(Optional.of(fragileCargo));
        when(distanceService.findRoute(req.getOriginAddress(), req.getDestAddress()))
                .thenReturn(new Route(12.3, 35, 37.566, 126.978, 37.500, 127.036));
        when(vehicleTypeService.findAllEntities()).thenReturn(List.of(smallVehicle, midVehicle));

        FareCalculateResponseDTO res = fareService.calculateFare(req);

        assertThat(res.getBaseFare()).isEqualTo(30000);
        assertThat(res.getDistanceFare()).isEqualTo(8000);
        assertThat(res.getCargoFare()).isEqualTo(5000);
        assertThat(res.getSurchargeFare()).isEqualTo(0);
        assertThat(res.getTotalFare()).isEqualTo(43000);
        assertThat(res.getRecommendedVehicleTypeId()).isEqualTo(1L);
        assertThat(res.isInquiryRequired()).isFalse();
    }

    @Test
    @DisplayName("예시 3: 심야·주말·중량초과 할증 및 100원 단위 절사")
    void calculateFare_example3_surchargesAndTruncation() {
        LocalDateTime pickupAt = LocalDateTime.of(2026, 11, 28, 23, 0); // 토요일 23시 (야간 + 주말)
        FareCalculateRequestDTO req = FareCalculateRequestDTO.builder()
                .originAddress("강원특별자치도 춘천시 한림대학길 1")
                .destAddress("서울특별시 중구 을지로 1")
                .vehicleTypeId(2L)
                .cargoTypeId(2L)
                .cargoWeightKg(900) // 1000kg의 80%(800kg) 초과 -> 중량 할증 포함
                .pickupAt(pickupAt)
                .build();

        // base 45000, ceil(15.1)=16 -> over 6km * 1500 = 9000, cargo 5000 => fare = 59000
        // 할증률: night(20) + weekend(10) + heavy(15) = 45% -> 59000 * 45 / 100 = 26550
        // total = 85550 -> 100원 미만 버림 => 85500, surchargeFare = 85500 - 59000 = 26500
        when(vehicleTypeService.findById(2L)).thenReturn(Optional.of(midVehicle));
        when(cargoTypeRepository.findById(2L)).thenReturn(Optional.of(fragileCargo));
        when(distanceService.findRoute(req.getOriginAddress(), req.getDestAddress()))
                .thenReturn(new Route(15.1, 90, 37.886, 127.738, 37.566, 126.982));
        when(vehicleTypeService.findAllEntities()).thenReturn(List.of(smallVehicle, midVehicle));

        FareCalculateResponseDTO res = fareService.calculateFare(req);

        assertThat(res.getBaseFare()).isEqualTo(45000);
        assertThat(res.getDistanceFare()).isEqualTo(9000);
        assertThat(res.getCargoFare()).isEqualTo(5000);
        assertThat(res.getTotalFare()).isEqualTo(85500);
        assertThat(res.getSurchargeFare()).isEqualTo(26500);
        assertThat(res.getRecommendedVehicleTypeId()).isEqualTo(2L);
        assertThat(res.isInquiryRequired()).isFalse();
    }
}