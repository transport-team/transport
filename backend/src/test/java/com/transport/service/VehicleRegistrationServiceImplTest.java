package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;

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
import com.transport.domain.VehicleRegistrationRequest;
import com.transport.domain.enums.VehicleRegistrationStatus;
import com.transport.dto.VehicleRegistrationRequestDTO;
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

	@Test
	@DisplayName("삭제 승인된 동일 차종과 차량번호는 새 등록 신청을 막지 않는다")
	void canApplyAgainAfterDeletionApproved() {
		VehicleType type = VehicleType.builder().id(2L).active(true).build();
		VehicleRegistrationRequestDTO input = new VehicleRegistrationRequestDTO();
		input.setVehicleTypeId(2L);
		input.setPlateNo("12가3456");
		when(driverRepository.findByMemberId(10L)).thenReturn(Optional.of(driver));
		when(vehicleTypeService.findById(2L)).thenReturn(Optional.of(type));
		when(requestRepository.save(any(VehicleRegistrationRequest.class))).thenAnswer(invocation -> {
			VehicleRegistrationRequest saved = invocation.getArgument(0);
			return VehicleRegistrationRequest.builder().id(20L).driver(saved.getDriver())
					.vehicleType(saved.getVehicleType()).plateNo(saved.getPlateNo())
					.status(saved.getStatus()).build();
		});

		Long id = vehicleRegistrationService.apply(10L, input);

		assertThat(id).isEqualTo(20L);
		verify(vehicleRepository).existsByDriverAndVehicleTypeAndStatusNot(driver, type, VehicleStatus.INACTIVE);
		verify(vehicleRepository).existsByPlateNoAndStatusNot("12가3456", VehicleStatus.INACTIVE);
		verify(requestRepository).save(any(VehicleRegistrationRequest.class));
	}

	@Test
	@DisplayName("삭제 승인이 안 된 동일 차종의 차량은 중복 신청을 막는다")
	void cannotApplyIfActiveOrUnderMaintenance() {
		VehicleType type = VehicleType.builder().id(2L).active(true).build();
		VehicleRegistrationRequestDTO input = new VehicleRegistrationRequestDTO();
		input.setVehicleTypeId(2L);
		input.setPlateNo("99나9999");
		when(driverRepository.findByMemberId(10L)).thenReturn(Optional.of(driver));
		when(vehicleTypeService.findById(2L)).thenReturn(Optional.of(type));
		when(vehicleRepository.existsByDriverAndVehicleTypeAndStatusNot(driver, type, VehicleStatus.INACTIVE))
				.thenReturn(true);

		assertThatThrownBy(() -> vehicleRegistrationService.apply(10L, input))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_DRIVER_VEHICLE_TYPE);
		verify(requestRepository, never()).save(any(VehicleRegistrationRequest.class));
	}

	@Test
	@DisplayName("다른 활성 차량이 사용하는 차량번호는 재등록할 수 없다")
	void cannotApplyWithActivePlate() {
		VehicleType type = VehicleType.builder().id(2L).active(true).build();
		VehicleRegistrationRequestDTO input = new VehicleRegistrationRequestDTO();
		input.setVehicleTypeId(2L);
		input.setPlateNo("12가3456");
		when(driverRepository.findByMemberId(10L)).thenReturn(Optional.of(driver));
		when(vehicleTypeService.findById(2L)).thenReturn(Optional.of(type));
		when(vehicleRepository.existsByPlateNoAndStatusNot("12가3456", VehicleStatus.INACTIVE))
				.thenReturn(true);

		assertThatThrownBy(() -> vehicleRegistrationService.apply(10L, input))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_VEHICLE_PLATE);
		verify(requestRepository, never()).save(any(VehicleRegistrationRequest.class));
	}

	@Test
	@DisplayName("동일 차종으로 승인 대기 중인 등록 신청이 있으면 재신청을 막는다")
	void cannotApplyWithPendingRegistration() {
		VehicleType type = VehicleType.builder().id(2L).active(true).build();
		VehicleRegistrationRequestDTO input = new VehicleRegistrationRequestDTO();
		input.setVehicleTypeId(2L);
		input.setPlateNo("12가3456");
		when(driverRepository.findByMemberId(10L)).thenReturn(Optional.of(driver));
		when(vehicleTypeService.findById(2L)).thenReturn(Optional.of(type));
		when(requestRepository.existsByDriverAndVehicleTypeAndStatus(
				driver, type, VehicleRegistrationStatus.PENDING)).thenReturn(true);

		assertThatThrownBy(() -> vehicleRegistrationService.apply(10L, input))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_DRIVER_VEHICLE_TYPE);
		verify(requestRepository, never()).save(any(VehicleRegistrationRequest.class));
	}

	@Test
	@DisplayName("과거 비활성 차량이 있어도 관리자 재등록 승인이 가능하다")
	void adminCanApproveReplacementVehicle() {
		VehicleType type = VehicleType.builder().id(2L).active(true).build();
		VehicleRegistrationRequest request = VehicleRegistrationRequest.builder()
				.id(20L).driver(driver).vehicleType(type).plateNo("12가3456")
				.status(VehicleRegistrationStatus.PENDING).build();
		when(requestRepository.findForReview(20L)).thenReturn(Optional.of(request));

		vehicleRegistrationService.approve(20L);

		verify(vehicleRepository).existsByDriverAndVehicleTypeAndStatusNot(driver, type, VehicleStatus.INACTIVE);
		verify(vehicleRepository).existsByPlateNoAndStatusNot("12가3456", VehicleStatus.INACTIVE);
		verify(vehicleRepository).save(any(Vehicle.class));
		assertThat(request.getStatus()).isEqualTo(VehicleRegistrationStatus.APPROVED);
	}

	@Test
	@DisplayName("관리자 승인 전에 동일 차종의 새 활성 차량이 생기면 중복 승인을 막는다")
	void adminRejectsApprovalIfActiveTypeAppears() {
		VehicleType type = VehicleType.builder().id(2L).active(true).build();
		VehicleRegistrationRequest request = VehicleRegistrationRequest.builder()
				.id(20L).driver(driver).vehicleType(type).plateNo("12가3456")
				.status(VehicleRegistrationStatus.PENDING).build();
		when(requestRepository.findForReview(20L)).thenReturn(Optional.of(request));
		when(vehicleRepository.existsByDriverAndVehicleTypeAndStatusNot(driver, type, VehicleStatus.INACTIVE))
				.thenReturn(true);

		assertThatThrownBy(() -> vehicleRegistrationService.approve(20L))
				.isInstanceOf(CustomException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_DRIVER_VEHICLE_TYPE);
		verify(vehicleRepository, never()).save(any(Vehicle.class));
	}

	private void givenMyVehicle() {
		when(driverRepository.findByMemberId(10L)).thenReturn(Optional.of(driver));
		when(vehicleRepository.findForUpdate(7L)).thenReturn(Optional.of(vehicle));
	}
}
