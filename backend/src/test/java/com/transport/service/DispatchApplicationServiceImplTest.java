package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.DispatchApplication;
import com.transport.domain.Driver;
import com.transport.domain.Shipment;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.domain.enums.VehicleStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.DriverScheduleRepository;
import com.transport.repository.ShipmentRepository;
import com.transport.repository.VehicleRepository;

@ExtendWith(MockitoExtension.class)
class DispatchApplicationServiceImplTest {

    @Mock
    private DispatchApplicationRepository applicationRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ShipmentRepository shipmentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DispatchRepository dispatchRepository;

    @Mock
    private DriverScheduleRepository driverScheduleRepository;

    @InjectMocks
    private DispatchApplicationServiceImpl dispatchService;

    @Test
    @DisplayName("예상 운송 시간이 없으면 배차 신청을 거부한다")
    void apply_rejectsShipmentWithoutDuration() {
        Long memberId = 10L;
        Long shipmentId = 20L;
        Long vehicleId = 30L;
        Driver driver = Driver.builder()
                .id(40L)
                .status(DriverStatus.ACTIVE)
                .build();
        VehicleType vehicleType = VehicleType.builder().id(50L).build();
        Vehicle vehicle = Vehicle.builder()
                .id(vehicleId)
                .driver(driver)
                .vehicleType(vehicleType)
                .status(VehicleStatus.AVAILABLE)
                .build();
        Shipment shipment = Shipment.builder()
                .id(shipmentId)
                .vehicleType(vehicleType)
                .status(ShipmentStatus.REQUESTED)
                .pickupAt(LocalDateTime.now().plusDays(1))
                .durationMin(null)
                .build();

        when(driverRepository.findByMemberId(memberId)).thenReturn(Optional.of(driver));
        when(shipmentRepository.findById(shipmentId)).thenReturn(Optional.of(shipment));
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(applicationRepository.existsByShipmentAndDriverAndStatus(
                shipment, driver, DispatchApplicationStatus.PENDING)).thenReturn(false);
        when(driverScheduleRepository.findByDriver_IdAndWorkDate(
                driver.getId(), shipment.getPickupAt().toLocalDate())).thenReturn(Optional.empty());

        CustomException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> dispatchService.apply(memberId, shipmentId, vehicleId),
                CustomException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(exception).hasMessage("예상 운송 시간이 제공되지 않은 배송은 배차를 신청할 수 없습니다.");
    }
}
