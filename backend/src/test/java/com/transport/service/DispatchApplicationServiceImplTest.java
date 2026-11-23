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

import org.junit.jupiter.api.DisplayName;
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

    // 배송이 취소되면 대기 중인 지원을 모두 자동 마감 (팀원 3 결제 취소가 부르는 약속 메서드)
    @Test
    void closePendingApplicationsWhenShipmentCanceled() {
        Shipment shipment = createShipment(ShipmentStatus.CANCELED);
        DispatchApplication first = createApplication(shipment, DispatchApplicationStatus.PENDING);
        DispatchApplication second = DispatchApplication.builder()
                .id(4L)
                .shipment(shipment)
                .status(DispatchApplicationStatus.PENDING)
                .build();

        when(applicationRepository.findByShipmentAndStatusOrderByCreatedAtAsc(shipment, DispatchApplicationStatus.PENDING))
                .thenReturn(List.of(first, second));

        dispatchApplicationService.closePendingApplications(shipment);

        assertThat(List.of(first, second))
                .extracting(DispatchApplication::getStatus)
                .containsOnly(DispatchApplicationStatus.NOT_SELECTED);
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

    // 배송이 끝나도 배차(active)는 남겨 두므로, 끝난 배송의 배차는 시간 겹침 검사에서 뺀다 (11-19 결정 34, #86)
    @Test
    @DisplayName("완료 · 취소된 배송의 배차는 시간이 겹쳐도 새 지원을 막지 않는다 (#86)")
    void finishedDispatchesDoNotBlockApply() {
        Driver driver = Driver.builder().id(5L).status(DriverStatus.ACTIVE).build();
        VehicleType vehicleType = VehicleType.builder().id(2L).build();
        Vehicle vehicle = Vehicle.builder()
                .id(7L).driver(driver).vehicleType(vehicleType).status(VehicleStatus.AVAILABLE).build();

        // 새로 지원하려는 배송 (10:00 ~ 11:00)
        Shipment shipment = Shipment.builder()
                .id(2L).status(ShipmentStatus.REQUESTED).vehicleType(vehicleType)
                .pickupAt(LocalDateTime.of(2026, 11, 15, 10, 0)).durationMin(60).build();

        // 같은 시간대(10:30 ~ 11:30)지만 이미 끝난 배송의 배차들 — active는 그대로 남아 있다
        Dispatch completed = activeDispatchOf(driver, 98L, ShipmentStatus.COMPLETED, LocalDateTime.of(2026, 11, 15, 10, 30));
        Dispatch canceled = activeDispatchOf(driver, 99L, ShipmentStatus.CANCELED, LocalDateTime.of(2026, 11, 15, 10, 30));

        when(driverRepository.findByMemberId(10L)).thenReturn(Optional.of(driver));
        when(shipmentService.getShipment(2L)).thenReturn(shipment);
        when(vehicleRepository.findById(7L)).thenReturn(Optional.of(vehicle));
        when(driverScheduleRepository.findByDriver_IdAndWorkDate(5L, shipment.getPickupAt().toLocalDate()))
                .thenReturn(Optional.empty());
        when(dispatchRepository.findByDriver_IdAndActiveTrue(5L)).thenReturn(List.of(completed, canceled));
        when(applicationRepository.save(any(DispatchApplication.class)))
                .thenAnswer(invocation -> DispatchApplication.builder().id(77L).build());

        Long applicationId = dispatchApplicationService.apply(10L, 2L, 7L);

        assertThat(applicationId).isEqualTo(77L);
    }

    @Test
    @DisplayName("운송 요청 목록: 진행 중인 배차와 겹치면 지원 불가, 끝난 배송의 배차와 겹치는 것은 지원 가능 (#86)")
    void availableShipmentsIgnoreFinishedDispatches() {
        Long memberId = 10L;
        Driver driver = Driver.builder().id(40L).status(DriverStatus.ACTIVE).build();
        VehicleType vehicleType = VehicleType.builder().id(50L).build();
        Vehicle vehicle = Vehicle.builder()
                .id(30L).driver(driver).vehicleType(vehicleType).status(VehicleStatus.AVAILABLE).build();
        LocalDateTime tomorrow10 = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0);
        LocalDateTime dayAfter10 = tomorrow10.plusDays(1);

        // 내일 10시 요청은 진행 중(배차 완료) 배차와, 모레 10시 요청은 이미 완료된 배송의 배차와 시간이 겹친다
        Shipment overlapsOngoing = Shipment.builder()
                .id(21L).vehicleType(vehicleType).status(ShipmentStatus.REQUESTED).pickupAt(tomorrow10).durationMin(60).build();
        Shipment overlapsFinished = Shipment.builder()
                .id(22L).vehicleType(vehicleType).status(ShipmentStatus.REQUESTED).pickupAt(dayAfter10).durationMin(60).build();
        Dispatch ongoing = activeDispatchOf(driver, 91L, ShipmentStatus.ASSIGNED, tomorrow10.plusMinutes(30));
        Dispatch finished = activeDispatchOf(driver, 92L, ShipmentStatus.COMPLETED, dayAfter10.plusMinutes(30));

        when(driverRepository.findByMemberId(memberId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriver_Id(40L)).thenReturn(List.of(vehicle));
        when(dispatchRepository.findByDriver_IdAndActiveTrue(40L)).thenReturn(List.of(ongoing, finished));
        when(applicationRepository.findByDriver_IdAndStatus(40L, DispatchApplicationStatus.PENDING)).thenReturn(List.of());
        when(applicationRepository.findByDriver_IdAndStatus(40L, DispatchApplicationStatus.REJECTED)).thenReturn(List.of());
        when(shipmentService.getRequestedShipments()).thenReturn(List.of(overlapsOngoing, overlapsFinished));
        when(driverScheduleRepository.findByDriver_IdAndWorkDate(any(), any())).thenReturn(Optional.empty());

        var result = dispatchApplicationService.getAvailableShipments(memberId);

        assertThat(result.get(0).isCanApply()).isFalse();
        assertThat(result.get(0).getBlockReasons()).contains("이미 확정된 배차와 운송 시간이 겹칩니다.");
        assertThat(result.get(1).getBlockReasons()).doesNotContain("이미 확정된 배차와 운송 시간이 겹칩니다.");
        assertThat(result.get(1).isCanApply()).isTrue();
    }

    // 기사의 활성 배차 (배송 상태 · 상차 시각 지정, 소요 60분)
    private Dispatch activeDispatchOf(Driver driver, Long shipmentId, ShipmentStatus status, LocalDateTime pickupAt) {
        Shipment assigned = Shipment.builder().id(shipmentId).status(status).pickupAt(pickupAt).durationMin(60).build();
        return Dispatch.builder().shipment(assigned).driver(driver).active(true).build();
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
        when(shipmentService.getShipment(shipmentId)).thenReturn(shipment);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(applicationRepository.existsByShipmentAndDriverAndStatus(
                shipment, driver, DispatchApplicationStatus.PENDING)).thenReturn(false);
        when(applicationRepository.existsByShipmentAndDriverAndStatus(
                shipment, driver, DispatchApplicationStatus.REJECTED)).thenReturn(false);
        when(driverScheduleRepository.findByDriver_IdAndWorkDate(
                driver.getId(), shipment.getPickupAt().toLocalDate())).thenReturn(Optional.empty());

        CustomException exception = org.assertj.core.api.Assertions.catchThrowableOfType(
                () -> dispatchApplicationService.apply(memberId, shipmentId, vehicleId),
                CustomException.class);

        assertThat(exception).isNotNull();
        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT);
        assertThat(exception).hasMessage("예상 운송 시간이 제공되지 않은 배송은 배차를 신청할 수 없습니다.");
    }

    @Test
    @DisplayName("고객이 거절한 운송 요청에는 다시 지원할 수 없다 (지원자 목록에 다시 나타나지 않게)")
    void cannotReapplyAfterRejectedByCustomer() {
        Long memberId = 10L;
        Long vehicleId = 30L;
        Driver driver = Driver.builder().id(40L).status(DriverStatus.ACTIVE).build();
        VehicleType vehicleType = VehicleType.builder().id(50L).build();
        Vehicle vehicle = Vehicle.builder()
                .id(vehicleId).driver(driver).vehicleType(vehicleType).status(VehicleStatus.AVAILABLE).build();
        Shipment shipment = Shipment.builder()
                .id(20L).vehicleType(vehicleType).status(ShipmentStatus.REQUESTED)
                .pickupAt(LocalDateTime.now().plusDays(1)).durationMin(60).build();

        when(driverRepository.findByMemberId(memberId)).thenReturn(Optional.of(driver));
        when(shipmentService.getShipment(20L)).thenReturn(shipment);
        when(vehicleRepository.findById(vehicleId)).thenReturn(Optional.of(vehicle));
        when(applicationRepository.existsByShipmentAndDriverAndStatus(
                shipment, driver, DispatchApplicationStatus.PENDING)).thenReturn(false);
        when(applicationRepository.existsByShipmentAndDriverAndStatus(
                shipment, driver, DispatchApplicationStatus.REJECTED)).thenReturn(true);

        assertThatThrownBy(() -> dispatchApplicationService.apply(memberId, 20L, vehicleId))
                .isInstanceOf(CustomException.class)
                .hasMessage("고객이 거절한 운송 요청에는 다시 지원할 수 없습니다.")
                .extracting("errorCode").isEqualTo(ErrorCode.DUPLICATE_DISPATCH_APPLICATION);
        verify(applicationRepository, never()).save(any(DispatchApplication.class));
    }

    @Test
    @DisplayName("운송 요청 목록: 거절된 요청은 지원 불가와 이유를, 소요시간이 없는 요청은 500 없이 지원 불가로 보여 준다")
    void availableShipmentsShowRejectedAndMissingDuration() {
        Long memberId = 10L;
        Driver driver = Driver.builder().id(40L).status(DriverStatus.ACTIVE).build();
        VehicleType vehicleType = VehicleType.builder().id(50L).build();
        Vehicle vehicle = Vehicle.builder()
                .id(30L).driver(driver).vehicleType(vehicleType).status(VehicleStatus.AVAILABLE).build();
        Shipment rejected = Shipment.builder()
                .id(21L).vehicleType(vehicleType).status(ShipmentStatus.REQUESTED)
                .pickupAt(LocalDateTime.now().plusDays(1)).durationMin(60).build();
        Shipment noDuration = Shipment.builder()
                .id(22L).vehicleType(vehicleType).status(ShipmentStatus.REQUESTED)
                .pickupAt(LocalDateTime.now().plusDays(2)).durationMin(null).build();

        when(driverRepository.findByMemberId(memberId)).thenReturn(Optional.of(driver));
        when(vehicleRepository.findByDriver_Id(40L)).thenReturn(List.of(vehicle));
        when(dispatchRepository.findByDriver_IdAndActiveTrue(40L)).thenReturn(List.of());
        when(applicationRepository.findByDriver_IdAndStatus(40L, DispatchApplicationStatus.PENDING)).thenReturn(List.of());
        when(applicationRepository.findByDriver_IdAndStatus(40L, DispatchApplicationStatus.REJECTED))
                .thenReturn(List.of(createApplication(rejected, DispatchApplicationStatus.REJECTED)));
        when(shipmentService.getRequestedShipments()).thenReturn(List.of(rejected, noDuration));
        when(driverScheduleRepository.findByDriver_IdAndWorkDate(any(), any())).thenReturn(Optional.empty());

        var result = dispatchApplicationService.getAvailableShipments(memberId);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).isCanApply()).isFalse();
        assertThat(result.get(0).isAlreadyApplied()).isFalse();
        assertThat(result.get(0).getBlockReasons()).contains("고객이 거절한 운송 요청에는 다시 지원할 수 없습니다.");
        assertThat(result.get(1).isCanApply()).isFalse();
        assertThat(result.get(1).getDurationMin()).isZero();
        assertThat(result.get(1).getBlockReasons()).contains("예상 운송 시간이 올바르지 않아 배차 신청할 수 없습니다.");
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
