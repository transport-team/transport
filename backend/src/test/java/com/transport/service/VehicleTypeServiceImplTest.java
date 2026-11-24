package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.VehicleType;
import com.transport.dto.VehicleTypeRequestDTO;
import com.transport.exception.CustomException;
import com.transport.repository.VehicleTypeRepository;

@ExtendWith(MockitoExtension.class)
class VehicleTypeServiceImplTest {

    @Mock
    private VehicleTypeRepository vehicleTypeRepository;

    @InjectMocks
    private VehicleTypeServiceImpl vehicleTypeService;

    private VehicleTypeRequestDTO request(String code) {
        return VehicleTypeRequestDTO.builder()
                .code(code).name("3.5톤").maxLoadKg(3500)
                .baseFare(95000).baseDistanceKm(10).perKmFare(2500)
                .nightRate(20).weekendRate(10).heavyRate(10).active(true)
                .build();
    }

    @Test
    void publicListExcludesInactiveTypes() {
        when(vehicleTypeRepository.findAll()).thenReturn(List.of(
                VehicleType.builder().id(1L).code("TON1").active(true).build(),
                VehicleType.builder().id(2L).code("OLD").active(false).build()));

        assertThat(vehicleTypeService.getVehicleTypes()).extracting("code").containsExactly("TON1");
    }

    @Test
    void adminListIncludesInactiveTypes() {
        when(vehicleTypeRepository.findAll()).thenReturn(List.of(
                VehicleType.builder().id(2L).code("OLD").active(false).build()));

        assertThat(vehicleTypeService.getAllVehicleTypesForAdmin()).hasSize(1);
    }

    @Test
    void duplicateCodeCannotBeRegistered() {
        when(vehicleTypeRepository.existsByCode("TON3_5")).thenReturn(true);

        assertThatThrownBy(() -> vehicleTypeService.registerVehicleType(request("TON3_5")))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void priceUpdateDoesNotChangeTypeCode() {
        VehicleType type = VehicleType.builder().id(5L).code("TON3_5")
                .name("기존 차종").baseFare(80000).active(true).build();
        when(vehicleTypeRepository.findById(5L)).thenReturn(Optional.of(type));

        vehicleTypeService.updateVehicleType(5L, request("TON3_5"));

        assertThat(type.getCode()).isEqualTo("TON3_5");
        assertThat(type.getBaseFare()).isEqualTo(95000);
        assertThat(type.getName()).isEqualTo("3.5톤");
    }

    @Test
    void activeStatusCanChangeWithoutDeletingHistoricalType() {
        VehicleType type = VehicleType.builder().id(5L).code("TON3_5").active(true).build();
        when(vehicleTypeRepository.findById(5L)).thenReturn(Optional.of(type));

        vehicleTypeService.changeVehicleTypeActive(5L, false);

        assertThat(type.isActive()).isFalse();
    }
}
