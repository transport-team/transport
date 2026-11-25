package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.DispatchApplication;
import com.transport.domain.Driver;
import com.transport.domain.DriverSchedule;
import com.transport.domain.Shipment;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.domain.enums.DriverScheduleStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverWorkHoursResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.DriverScheduleRepository;

// 기사 근무 가능 시간 (DRV-01): 오늘 이후 · 휴무일과 따로 · 자정 넘기는 근무 · 등록과 수정 (DB 없이)
@ExtendWith(MockitoExtension.class)
class DriverDayOffServiceImplTest {

    private static final Long MEMBER_ID = 10L;
    private static final Long DRIVER_ID = 5L;

    @Mock
    DriverRepository driverRepository;

    @Mock
    DriverScheduleRepository scheduleRepository;

    @Mock
    DispatchRepository dispatchRepository;

    @Mock
    DispatchApplicationRepository applicationRepository;

    @Mock
    StatusHistoryService statusHistoryService;

    @InjectMocks
    DriverDayOffServiceImpl dayOffService;

    private final Driver driver = Driver.builder().id(DRIVER_ID).build();
    private final LocalDate tomorrow = LocalDate.now().plusDays(1);

    private void givenDriver() {
        when(driverRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(driver));
    }

    private DriverSchedule schedule(DriverScheduleStatus status, LocalTime start, LocalTime end) {
        return DriverSchedule.builder()
                .id(1L).driver(driver).workDate(tomorrow).startTime(start).endTime(end).status(status).build();
    }

    @Test
    @DisplayName("근무 시간이 없는 날이면 새로 저장한다 (분 단위로 맞춤)")
    void saveNewWorkHours() {
        givenDriver();
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(DriverSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverWorkHoursResponseDTO result = dayOffService.saveWorkHours(
                MEMBER_ID, tomorrow, LocalTime.of(9, 0, 30), LocalTime.of(18, 0));

        ArgumentCaptor<DriverSchedule> saved = ArgumentCaptor.forClass(DriverSchedule.class);
        verify(scheduleRepository).save(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(DriverScheduleStatus.AVAILABLE);
        assertThat(saved.getValue().getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(result.date()).isEqualTo(tomorrow);
        assertThat(result.endTime()).isEqualTo(LocalTime.of(18, 0));
        assertThat(result.overnight()).isFalse();
    }

    @Test
    @DisplayName("이미 근무 시간이 있으면 새로 만들지 않고 시간만 바꾼다")
    void updateExistingWorkHours() {
        givenDriver();
        DriverSchedule existing = schedule(DriverScheduleStatus.AVAILABLE, LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow)).thenReturn(Optional.of(existing));

        dayOffService.saveWorkHours(MEMBER_ID, tomorrow, LocalTime.of(13, 0), LocalTime.of(20, 0));

        assertThat(existing.getStartTime()).isEqualTo(LocalTime.of(13, 0));
        assertThat(existing.getEndTime()).isEqualTo(LocalTime.of(20, 0));
        verify(scheduleRepository, never()).save(any(DriverSchedule.class));
    }

    @Test
    @DisplayName("끝이 시작보다 이르면 자정을 넘기는 근무로 저장한다")
    void overnightWorkHours() {
        givenDriver();
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow)).thenReturn(Optional.empty());
        when(scheduleRepository.save(any(DriverSchedule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DriverWorkHoursResponseDTO result = dayOffService.saveWorkHours(
                MEMBER_ID, tomorrow, LocalTime.of(22, 0), LocalTime.of(6, 0));

        assertThat(result.overnight()).isTrue();
    }

    @Test
    @DisplayName("휴무일은 해당 날짜의 근무시간 예외로 한 번에 전환할 수 있다")
    void offDayCanChangeToWorkHours() {
        givenDriver();
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow))
                .thenReturn(Optional.of(schedule(DriverScheduleStatus.OFF, LocalTime.MIN, LocalTime.MAX)));

        DriverWorkHoursResponseDTO result = dayOffService.saveWorkHours(
                MEMBER_ID, tomorrow, LocalTime.of(9, 0), LocalTime.of(18, 0));
        assertThat(result.startTime()).isEqualTo(LocalTime.of(9, 0));
        verify(scheduleRepository, never()).save(any(DriverSchedule.class));
    }

    @Test
    @DisplayName("지난 날짜 · 시작과 끝이 같은 시간은 400")
    void invalidDateOrSameTime() {
        givenDriver();

        assertThatThrownBy(() -> dayOffService.saveWorkHours(
                MEMBER_ID, LocalDate.now().minusDays(1), LocalTime.of(9, 0), LocalTime.of(18, 0)))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> dayOffService.saveWorkHours(
                MEMBER_ID, tomorrow, LocalTime.of(9, 0), LocalTime.of(9, 0, 40)))
                .isInstanceOf(CustomException.class)
                .hasMessage("시작 시간과 끝 시간이 같을 수 없습니다.");
    }

    @Test
    @DisplayName("근무 시간 삭제: 근무 가능 일정만 지우고, 휴무일 · 없는 날은 막는다")
    void deleteWorkHours() {
        givenDriver();
        DriverSchedule work = schedule(DriverScheduleStatus.AVAILABLE, LocalTime.of(9, 0), LocalTime.of(18, 0));
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow)).thenReturn(Optional.of(work));

        dayOffService.deleteWorkHours(MEMBER_ID, tomorrow);

        verify(scheduleRepository).delete(work);
    }

    @Test
    @DisplayName("휴무일을 근무 시간 삭제로 지울 수 없고, 없는 날은 404")
    void deleteWorkHoursRejectsOffDayAndMissing() {
        givenDriver();
        LocalDate dayAfter = tomorrow.plusDays(1);
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow))
                .thenReturn(Optional.of(schedule(DriverScheduleStatus.OFF, LocalTime.MIN, LocalTime.MAX)));
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, dayAfter)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> dayOffService.deleteWorkHours(MEMBER_ID, tomorrow))
                .isInstanceOf(CustomException.class)
                .hasMessage("휴무일은 휴무 취소로 지워 주세요.");
        assertThatThrownBy(() -> dayOffService.deleteWorkHours(MEMBER_ID, dayAfter))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
    }

    @Test
    @DisplayName("월별 근무 시간은 근무 가능 일정만 준다 (휴무는 빼고)")
    void getWorkHoursSkipsOffDays() {
        givenDriver();
        LocalDate first = LocalDate.of(2026, 12, 1);
        DriverSchedule work = DriverSchedule.builder().id(1L).driver(driver).workDate(first)
                .startTime(LocalTime.of(9, 0)).endTime(LocalTime.of(18, 0)).status(DriverScheduleStatus.AVAILABLE).build();
        DriverSchedule off = DriverSchedule.builder().id(2L).driver(driver).workDate(first.plusDays(1))
                .startTime(LocalTime.MIN).endTime(LocalTime.MAX).status(DriverScheduleStatus.OFF).build();
        when(scheduleRepository.findByDriver_IdAndWorkDateBetweenOrderByWorkDateAsc(
                DRIVER_ID, first, LocalDate.of(2026, 12, 31))).thenReturn(List.of(work, off));

        List<DriverWorkHoursResponseDTO> result = dayOffService.getWorkHours(MEMBER_ID, 2026, 12);

        assertThat(result).extracting(DriverWorkHoursResponseDTO::date).containsExactly(first);
    }

    @Test
    @DisplayName("예외 근무시간이 있어도 충돌이 없다면 휴무로 전환한다")
    void offDayOnWorkHoursDate() {
        givenDriver();
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow))
                .thenReturn(Optional.of(schedule(DriverScheduleStatus.AVAILABLE, LocalTime.of(9, 0), LocalTime.of(18, 0))));

        DriverSchedule existing = scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow).orElseThrow();
        dayOffService.registerOffDay(MEMBER_ID, tomorrow);
        assertThat(existing.getStatus()).isEqualTo(DriverScheduleStatus.OFF);
        verify(scheduleRepository, never()).save(any(DriverSchedule.class));
    }

    @Test
    @DisplayName("운송을 완료한 날은 휴무로 못 바꾼다 — 완료 시각은 팀원 4 StatusHistoryService로 묻는다")
    void completedDayBlocksOffDay() {
        givenDriver();
        Shipment shipment = Shipment.builder().id(30L).status(ShipmentStatus.COMPLETED).build();
        DispatchApplication selected = DispatchApplication.builder()
                .id(3L).driver(driver).shipment(shipment).status(DispatchApplicationStatus.SELECTED).build();
        when(scheduleRepository.findByDriver_IdAndWorkDate(DRIVER_ID, tomorrow)).thenReturn(Optional.empty());
        when(dispatchRepository.findByDriver_IdAndActiveTrue(DRIVER_ID)).thenReturn(List.of());
        when(applicationRepository.findByDriver_IdOrderByUpdatedAtDesc(DRIVER_ID)).thenReturn(List.of(selected));
        when(statusHistoryService.getLastChangedAt(30L, ShipmentStatus.COMPLETED))
                .thenReturn(Optional.of(tomorrow.atTime(10, 0)));

        assertThatThrownBy(() -> dayOffService.registerOffDay(MEMBER_ID, tomorrow))
                .isInstanceOf(CustomException.class)
                .hasMessage("배송을 완료한 날짜에는 휴무를 등록할 수 없습니다.");
        verify(scheduleRepository, never()).save(any());
    }
}
