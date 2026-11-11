package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Dispatch;
import com.transport.domain.DispatchApplication;
import com.transport.domain.Driver;
import com.transport.domain.DriverSchedule;
import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.domain.enums.DriverScheduleStatus;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.domain.enums.VehicleStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.DriverScheduleRepository;
import com.transport.repository.VehicleRepository;

// 고객의 배차 지원 거절 기능 테스트 (실제 DB 사용 안 함)
@ExtendWith(MockitoExtension.class)
class DispatchApplicationServiceImplTest {

    @Mock
    private DispatchApplicationRepository applicationRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ShipmentService shipmentService;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private DispatchRepository dispatchRepository;

    @Mock
    private DriverScheduleRepository driverScheduleRepository;

    @Mock
    private StatusHistoryService statusHistoryService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private DispatchApplicationServiceImpl dispatchApplicationService;

    // 고객이 자신의 배송에 들어온 대기 중 신청을 거절
    @Test
    void rejectPendingApplication() {
        Shipment shipment = createShipment(ShipmentStatus.REQUESTED);
        DispatchApplication application = createApplication(
                shipment, DispatchApplicationStatus.PENDING);

        when(shipmentService.getShipment(2L)).thenReturn(shipment);
        when(applicationRepository.findById(3L))
                .thenReturn(Optional.of(application));

        dispatchApplicationService.rejectApplicant(1L, 2L, 3L);

        assertThat(application.getStatus())
                .isEqualTo(DispatchApplicationStatus.REJECTED);

        verify(shipmentService).getShipment(2L);
        verify(applicationRepository).findById(3L);
    }

    // 다른 고객의 배송에 들어온 지원은 거절할 수 없음
    @Test
    void cannotRejectAnotherCustomersApplication() {
        Shipment shipment = createShipment(ShipmentStatus.REQUESTED);

        when(shipmentService.getShipment(2L)).thenReturn(shipment);

        assertThatThrownBy(() ->
                dispatchApplicationService.rejectApplicant(99L, 2L, 3L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.FORBIDDEN);

        verifyNoInteractions(applicationRepository);
    }

    // 이미 거절한 신청을 다시 거절할 수 없음
    @Test
    void cannotRejectAlreadyRejectedApplication() {
        Shipment shipment = createShipment(ShipmentStatus.REQUESTED);
        DispatchApplication application = createApplication(
                shipment, DispatchApplicationStatus.REJECTED);

        when(shipmentService.getShipment(2L)).thenReturn(shipment);
        when(applicationRepository.findById(3L))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() ->
                dispatchApplicationService.rejectApplicant(1L, 2L, 3L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPATCH_APPLICATION_NOT_PENDING);

        assertThat(application.getStatus())
                .isEqualTo(DispatchApplicationStatus.REJECTED);
    }

    // 다른 배송에 들어온 신청은 거절할 수 없음
    @Test
    void cannotRejectApplicationFromAnotherShipment() {
        Shipment shipment = createShipment(ShipmentStatus.REQUESTED);

        Shipment otherShipment = Shipment.builder()
                .id(99L)
                .build();

        DispatchApplication application = createApplication(
                otherShipment, DispatchApplicationStatus.PENDING);

        when(shipmentService.getShipment(2L)).thenReturn(shipment);
        when(applicationRepository.findById(3L))
                .thenReturn(Optional.of(application));

        assertThatThrownBy(() ->
                dispatchApplicationService.rejectApplicant(1L, 2L, 3L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThat(application.getStatus())
                .isEqualTo(DispatchApplicationStatus.PENDING);
    }

    // 이미 배차가 확정된 배송의 지원은 거절할 수 없음
    @Test
    void cannotRejectAfterShipmentAssigned() {
        Shipment shipment = createShipment(ShipmentStatus.ASSIGNED);

        when(shipmentService.getShipment(2L)).thenReturn(shipment);

        assertThatThrownBy(() ->
                dispatchApplicationService.rejectApplicant(1L, 2L, 3L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.SHIPMENT_NOT_REQUESTED);

        verifyNoInteractions(applicationRepository);
    }

    // 고객이 기사를 선택하면 선택된 기사 회원에게 배차 확정 알림을 보냄
    @Test
    void notifySelectedDriverWhenDispatchConfirmed() {
        // 1. 테스트용 고객 및 기사 회원
        Member customer = Member.builder()
                .id(1L)
                .build();

        Member driverMember = Member.builder()
                .id(10L)
                .build();

        // 2. 기사 및 차량
        Driver driver = Driver.builder()
                .id(5L)
                .member(driverMember)
                .status(DriverStatus.ACTIVE)
                .build();

        VehicleType vehicleType = VehicleType.builder()
                .id(2L)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(7L)
                .driver(driver)
                .vehicleType(vehicleType)
                .status(VehicleStatus.AVAILABLE)
                .build();

        // 3. 배차 대기 중인 배송
        Shipment shipment = Shipment.builder()
                .id(2L)
                .customer(customer)
                .vehicleType(vehicleType)
                .status(ShipmentStatus.REQUESTED)
                .pickupAt(LocalDateTime.of(2026, 11, 15, 10, 0))
                .durationMin(60)
                .build();

        // 4. 기사가 지원한 신청
        DispatchApplication application = DispatchApplication.builder()
                .id(3L)
                .shipment(shipment)
                .driver(driver)
                .vehicle(vehicle)
                .status(DispatchApplicationStatus.PENDING)
                .build();

        // 5. Repository와 Service의 가짜 응답 준비
        when(shipmentService.getShipment(2L))
                .thenReturn(shipment);

        when(applicationRepository.findById(3L))
                .thenReturn(Optional.of(application));

        when(applicationRepository.findByShipmentAndStatusOrderByCreatedAtAsc(
                shipment, DispatchApplicationStatus.PENDING))
                .thenReturn(List.of(application));

        // 6. 고객이 기사 선택
        dispatchApplicationService.selectApplicant(1L, 2L, 3L);

        // 7. 배차 확정 및 배송 상태 변경 확인
        assertThat(application.getStatus())
                .isEqualTo(DispatchApplicationStatus.SELECTED);

        assertThat(shipment.getStatus())
                .isEqualTo(ShipmentStatus.ASSIGNED);

        // 8. 배송 상태 변경 이력 저장 호출 확인
        verify(statusHistoryService).record(
                shipment,
                ShipmentStatus.REQUESTED,
                ShipmentStatus.ASSIGNED,
                customer,
                "고객이 지원 기사를 선택하여 배차 확정"
        );

        // 9. 선택된 기사 회원에게 알림 호출 확인
        verify(notificationService).send(
                driverMember,
                NotificationType.DISPATCHED,
                "배송 2번의 배차가 확정되었습니다.",
                shipment
        );
    }

    // 기존 확정 배차와 시간이 겹치면 새 배차 신청 불가
    @Test
    void cannotApplyWhenDispatchTimeOverlaps() {

        // 1. 승인된 기사
        Driver driver = Driver.builder()
                .id(5L)
                .status(DriverStatus.ACTIVE)
                .build();

        // 2. 차량
        VehicleType vehicleType = VehicleType.builder()
                .id(2L)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(7L)
                .driver(driver)
                .vehicleType(vehicleType)
                .status(VehicleStatus.AVAILABLE)
                .build();

        // 3. 새로 지원하려는 배송 (10:00 ~ 11:00)
        Shipment shipment = Shipment.builder()
                .id(2L)
                .status(ShipmentStatus.REQUESTED)
                .vehicleType(vehicleType)
                .pickupAt(LocalDateTime.of(2026, 11, 15, 10, 0))
                .durationMin(60)
                .build();

        // 4. 이미 확정된 배송 (10:30 ~ 11:30)
        Shipment assignedShipment = Shipment.builder()
                .id(99L)
                .pickupAt(LocalDateTime.of(2026, 11, 15, 10, 30))
                .durationMin(60)
                .build();

        Dispatch existingDispatch = Dispatch.builder()
                .shipment(assignedShipment)
                .driver(driver)
                .active(true)
                .build();

        // 5. Repository와 Service의 가짜 응답
        when(driverRepository.findByMemberId(10L))
                .thenReturn(Optional.of(driver));

        when(shipmentService.getShipment(2L))
                .thenReturn(shipment);

        when(vehicleRepository.findById(7L))
                .thenReturn(Optional.of(vehicle));

        when(driverScheduleRepository.findByDriver_IdAndWorkDate(
                5L, shipment.getPickupAt().toLocalDate()))
                .thenReturn(Optional.empty());

        when(dispatchRepository.findByDriver_IdAndActiveTrue(5L))
                .thenReturn(List.of(existingDispatch));

        // 6. 시간이 겹치므로 예외 발생
        assertThatThrownBy(() ->
                dispatchApplicationService.apply(10L, 2L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.DISPATCH_TIME_CONFLICT);

        // 7. 실제 배차 신청은 저장되지 않아야 함
        verify(applicationRepository, never())
                .save(any(DispatchApplication.class));
    }

    // 기사 휴무일(OFF)에는 배차 신청 불가
    @Test
    void cannotApplyOnDriverDayOff() {

        // 1. 승인된 기사
        Driver driver = Driver.builder()
                .id(5L)
                .status(DriverStatus.ACTIVE)
                .build();

        // 2. 기사 소유 차량
        VehicleType vehicleType = VehicleType.builder()
                .id(2L)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(7L)
                .driver(driver)
                .vehicleType(vehicleType)
                .status(VehicleStatus.AVAILABLE)
                .build();

        // 3. 11월 15일 상차 예정인 배송
        Shipment shipment = Shipment.builder()
                .id(2L)
                .status(ShipmentStatus.REQUESTED)
                .vehicleType(vehicleType)
                .pickupAt(LocalDateTime.of(2026, 11, 15, 10, 0))
                .durationMin(60)
                .build();

        // 4. 해당 날짜의 기사 근무 상태가 OFF
        DriverSchedule schedule = DriverSchedule.builder()
                .driver(driver)
                .workDate(shipment.getPickupAt().toLocalDate())
                .startTime(java.time.LocalTime.of(9, 0))
                .endTime(java.time.LocalTime.of(18, 0))
                .status(DriverScheduleStatus.OFF)
                .build();

        // 5. Repository와 Service의 가짜 응답
        when(driverRepository.findByMemberId(10L))
                .thenReturn(Optional.of(driver));

        when(shipmentService.getShipment(2L))
                .thenReturn(shipment);

        when(vehicleRepository.findById(7L))
                .thenReturn(Optional.of(vehicle));

        when(driverScheduleRepository.findByDriver_IdAndWorkDate(
                5L, shipment.getPickupAt().toLocalDate()))
                .thenReturn(Optional.of(schedule));

        // 6. 휴무일이므로 배차 신청 예외 발생
        assertThatThrownBy(() ->
                dispatchApplicationService.apply(10L, 2L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        // 7. 배차 신청이 저장되지 않아야 함
        verify(applicationRepository, never())
                .save(any(DispatchApplication.class));

        // 8. 휴무일 검사에서 차단되므로 시간 중복 검사는 진행하지 않음
        verifyNoInteractions(dispatchRepository);
    }

    // 예상 운송 시간이 0분이면 배차 신청 불가
    @Test
    void cannotApplyWhenDurationIsZero() {

        // 1. 승인된 기사
        Driver driver = Driver.builder()
                .id(5L)
                .status(DriverStatus.ACTIVE)
                .build();

        // 2. 기사 소유 차량
        VehicleType vehicleType = VehicleType.builder()
                .id(2L)
                .build();

        Vehicle vehicle = Vehicle.builder()
                .id(7L)
                .driver(driver)
                .vehicleType(vehicleType)
                .status(VehicleStatus.AVAILABLE)
                .build();

        // 3. 예상 운송 시간이 0분인 배송
        Shipment shipment = Shipment.builder()
                .id(2L)
                .status(ShipmentStatus.REQUESTED)
                .vehicleType(vehicleType)
                .pickupAt(LocalDateTime.of(2026, 11, 15, 10, 0))
                .durationMin(0)
                .build();

        // 4. Repository와 Service의 가짜 응답
        when(driverRepository.findByMemberId(10L))
                .thenReturn(Optional.of(driver));

        when(shipmentService.getShipment(2L))
                .thenReturn(shipment);

        when(vehicleRepository.findById(7L))
                .thenReturn(Optional.of(vehicle));

        when(driverScheduleRepository.findByDriver_IdAndWorkDate(
                5L, shipment.getPickupAt().toLocalDate()))
                .thenReturn(Optional.empty());

        // 5. 운송 시간이 올바르지 않으므로 예외 발생
        assertThatThrownBy(() ->
                dispatchApplicationService.apply(10L, 2L, 7L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_INPUT);

        // 6. 배차 신청 저장되지 않음
        verify(applicationRepository, never())
                .save(any(DispatchApplication.class));

        // 7. 시간 중복 검사까지 진행하지 않음
        verifyNoInteractions(dispatchRepository);
    }

    // 테스트용 배송 생성
    private Shipment createShipment(ShipmentStatus status) {
        Member customer = Member.builder()
                .id(1L)
                .build();

        return Shipment.builder()
                .id(2L)
                .customer(customer)
                .status(status)
                .build();
    }

    // 테스트용 배차 신청 생성
    private DispatchApplication createApplication(
            Shipment shipment, DispatchApplicationStatus status) {

        return DispatchApplication.builder()
                .id(3L)
                .shipment(shipment)
                .status(status)
                .build();
    }
}
