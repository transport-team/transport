package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Driver;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.domain.enums.VehicleStatus;
import com.transport.dto.DriverVehicleResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchRepository;
import com.transport.repository.VehicleDeletionRequestRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.VehicleRegistrationRequestRepository;
import com.transport.repository.VehicleRepository;

// 기사 차량 '정비 중' 변경 (#86, 11-19 결정 34): 배송이 끝나도 배차는 남으므로 완료 · 취소 배송의 배차는 빼고 센다
@ExtendWith(MockitoExtension.class)
class VehicleRegistrationServiceImplTest {

	private static final List<ShipmentStatus> FINISHED = List.of(ShipmentStatus.COMPLETED, ShipmentStatus.CANCELED);

	@Mock
	VehicleRegistrationRequestRepository requestRepository;

	@Mock
	DriverRepository driverRepository;

	@Mock
	VehicleRepository vehicleRepository;

	@Mock
	VehicleTypeService vehicleTypeService;

	@Mock
	DispatchRepository dispatchRepository;

	@Mock
	VehicleDeletionRequestRepository deletionRequestRepository;

	@InjectMocks
	VehicleRegistrationServiceImpl vehicleRegistrationService;

	private final Driver driver = Driver.builder().id(5L).status(DriverStatus.ACTIVE).build();
	private final Vehicle vehicle = Vehicle.builder()
			.id(7L).plateNo("12가3456").driver(driver)
			.vehicleType(VehicleType.builder().id(2L).build())
			.status(VehicleStatus.AVAILABLE)
			.build();

	@Test
	@DisplayName("끝난 배송의 배차만 남아 있으면 정비 중으로 바꿀 수 있다")
	void maintenanceAllowedWhenOnlyFinishedDispatches() {
		givenMyVehicle();
		when(dispatchRepository.existsByVehicle_IdAndActiveTrueAndShipment_StatusNotIn(7L, FINISHED)).thenReturn(false);

		DriverVehicleResponseDTO result = vehicleRegistrationService.changeMyVehicleStatus(10L, 7L, VehicleStatus.MAINTENANCE);

		assertThat(result.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
		assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.MAINTENANCE);
	}

	@Test
	@DisplayName("진행 중인 배송의 배차가 있으면 정비 중으로 바꿀 수 없다")
	void maintenanceBlockedWhenOngoingDispatch() {
		givenMyVehicle();
		when(dispatchRepository.existsByVehicle_IdAndActiveTrueAndShipment_StatusNotIn(7L, FINISHED)).thenReturn(true);

		assertThatThrownBy(() -> vehicleRegistrationService.changeMyVehicleStatus(10L, 7L, VehicleStatus.MAINTENANCE))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		assertThat(vehicle.getStatus()).isEqualTo(VehicleStatus.AVAILABLE);
	}

	private void givenMyVehicle() {
		when(driverRepository.findByMemberId(10L)).thenReturn(Optional.of(driver));
		when(vehicleRepository.findForUpdate(7L)).thenReturn(Optional.of(vehicle));
	}
}
