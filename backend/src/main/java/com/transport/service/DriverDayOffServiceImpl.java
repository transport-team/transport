package com.transport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.DateTimeException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;

// 기사 휴무일 관리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverDayOffServiceImpl implements DriverDayOffService {

    private final DriverRepository driverRepository;
    private final DriverScheduleRepository scheduleRepository;
    private final DispatchRepository dispatchRepository;
    private final DispatchApplicationRepository applicationRepository;
    private final StatusHistoryService statusHistoryService;  // 팀원 4 약속 메서드 (운송완료 시각)

    // 로그인 회원 ID로 기사 ID 조회
    private Long getDriverId(Long memberId) {
        return driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "기사 정보를 찾을 수 없습니다."))
                .getId();
    }

    // 조회할 연 · 월 (잘못된 값이면 400)
    private YearMonth toYearMonth(int year, int month) {
        try {
            return YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "조회할 연도 또는 월이 올바르지 않습니다.");
        }
    }

    // 월별 휴무일 조회
    @Override
    public List<LocalDate> getOffDays(Long memberId, int year, int month) {
        Long driverId = getDriverId(memberId);
        YearMonth yearMonth = toYearMonth(year, month);

        return scheduleRepository
                .findByDriver_IdAndWorkDateBetweenOrderByWorkDateAsc(
                        driverId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth())
                .stream()
                .filter(schedule -> schedule.getStatus() == DriverScheduleStatus.OFF)
                .map(DriverSchedule::getWorkDate)
                .toList();
    }

    // 운송 시간이 휴무일과 겹치는지 확인
    private boolean overlapsDate(Shipment shipment, LocalDate date) {
        LocalDateTime pickupAt = shipment.getPickupAt();
        if (pickupAt == null)
            return false;

        Integer durationMin = shipment.getDurationMin();
        if (durationMin == null || durationMin <= 0) {
            return pickupAt.toLocalDate().equals(date);
        }

        LocalDateTime endAt = pickupAt.plusMinutes(durationMin);

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        return pickupAt.isBefore(dayEnd) && endAt.isAfter(dayStart);
    }

    // 휴무일 등록
    @Transactional
    @Override
    public void registerOffDay(Long memberId, LocalDate date) {
        Long driverId = getDriverId(memberId);

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "오늘 이후의 날짜만 휴무로 등록할 수 있습니다.");
        }

        // 기존 근무 일정이 있으면 덮어쓰지 않음
        var existing = scheduleRepository
                .findByDriver_IdAndWorkDate(driverId, date);

        if (existing.isPresent()) {
            if (existing.get().getStatus() == DriverScheduleStatus.OFF) {
                return;
            }

            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "근무 시간이 등록된 날짜입니다. 근무 시간을 지운 뒤 휴무로 등록해 주세요.");
        }

        // 진행 중인 확정 배차와 겹치면 휴무 등록 불가
        boolean assignedConflict = dispatchRepository
                .findByDriver_IdAndActiveTrue(driverId)
                .stream()
                .filter(dispatch -> {
                    ShipmentStatus status = dispatch.getShipment().getStatus();
                    return status == ShipmentStatus.ASSIGNED
                            || status == ShipmentStatus.PICKED_UP
                            || status == ShipmentStatus.IN_TRANSIT
                            || status == ShipmentStatus.ARRIVED;
                })
                .anyMatch(dispatch -> overlapsDate(dispatch.getShipment(), date));

        if (assignedConflict) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "확정된 배차 일정과 겹쳐 휴무를 등록할 수 없습니다.");
        }

        // 신청 대기 중인 운송과 겹쳐도 휴무 등록 불가
        boolean pendingConflict = applicationRepository
                .findByDriver_IdOrderByUpdatedAtDesc(driverId)
                .stream()
                .filter(application -> application.getStatus() == DispatchApplicationStatus.PENDING
                        && application.getShipment().getStatus() == ShipmentStatus.REQUESTED)
                .anyMatch(application -> overlapsDate(application.getShipment(), date));

        if (pendingConflict) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "신청 대기 중인 운송이 있어 휴무를 등록할 수 없습니다.");
        }

        // 실제 배송 완료한 날짜에는 휴무 등록 불가
        boolean completedConflict = applicationRepository
                .findByDriver_IdOrderByUpdatedAtDesc(driverId)
                .stream()
                .filter(application -> application.getStatus() == DispatchApplicationStatus.SELECTED
                        && application.getShipment().getStatus() == ShipmentStatus.COMPLETED)
                .anyMatch(application -> statusHistoryService
                        .getLastChangedAt(application.getShipment().getId(), ShipmentStatus.COMPLETED)
                        .map(completedAt -> completedAt.toLocalDate().equals(date))
                        .orElse(false));

        if (completedConflict) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "배송을 완료한 날짜에는 휴무를 등록할 수 없습니다.");
        }

        DriverSchedule schedule = DriverSchedule.builder()
                .driver(driverRepository.findByMemberId(memberId)
                        .orElseThrow(() -> new CustomException(
                                ErrorCode.NOT_FOUND,
                                "기사 정보를 찾을 수 없습니다.")))
                .workDate(date)
                .startTime(LocalTime.MIN)
                .endTime(LocalTime.MAX)
                .status(DriverScheduleStatus.OFF)
                .build();

        scheduleRepository.save(schedule);
    }

    // 휴무일 취소
    @Transactional
    @Override
    public void cancelOffDay(Long memberId, LocalDate date) {
        Long driverId = getDriverId(memberId);

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "지난 날짜의 휴무는 취소할 수 없습니다.");
        }

        DriverSchedule schedule = scheduleRepository
                .findByDriver_IdAndWorkDate(driverId, date)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "등록된 휴무일이 없습니다."));

        if (schedule.getStatus() != DriverScheduleStatus.OFF) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "휴무일이 아닌 근무 일정은 삭제할 수 없습니다.");
        }

        scheduleRepository.delete(schedule);
    }

    // ===== 근무 가능 시간 (DRV-01) — 같은 날짜에 휴무와 근무 시간은 하나만 둔다 =====

    // 월별 근무 가능 시간 조회 (휴무는 getOffDays)
    @Override
    public List<DriverWorkHoursResponseDTO> getWorkHours(Long memberId, int year, int month) {
        Long driverId = getDriverId(memberId);
        YearMonth yearMonth = toYearMonth(year, month);

        return scheduleRepository
                .findByDriver_IdAndWorkDateBetweenOrderByWorkDateAsc(
                        driverId,
                        yearMonth.atDay(1),
                        yearMonth.atEndOfMonth())
                .stream()
                .filter(schedule -> schedule.getStatus() == DriverScheduleStatus.AVAILABLE)
                .map(DriverWorkHoursResponseDTO::from)
                .toList();
    }

    // 근무 가능 시간 등록 · 수정 — 오늘 이후 날짜만, 휴무일이면 휴무를 먼저 취소
    // 끝이 시작보다 이르면 자정을 넘기는 근무로 본다 (운송 요청 목록의 근무 종료 계산과 같다)
    @Transactional
    @Override
    public DriverWorkHoursResponseDTO saveWorkHours(
            Long memberId, LocalDate date, LocalTime startTime, LocalTime endTime) {

        Driver driver = driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "기사 정보를 찾을 수 없습니다."));

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "오늘 이후의 날짜만 근무 시간을 등록할 수 있습니다.");
        }
        if (startTime == null || endTime == null) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "시작 시간과 끝 시간을 모두 입력해 주세요.");
        }

        // 분 단위로만 저장 (화면의 시간 입력은 분까지)
        LocalTime start = startTime.withSecond(0).withNano(0);
        LocalTime end = endTime.withSecond(0).withNano(0);
        if (start.equals(end)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "시작 시간과 끝 시간이 같을 수 없습니다.");
        }

        var existing = scheduleRepository.findByDriver_IdAndWorkDate(driver.getId(), date);
        if (existing.isPresent()) {
            DriverSchedule schedule = existing.get();
            if (schedule.getStatus() == DriverScheduleStatus.OFF) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT,
                        "휴무일입니다. 휴무를 취소한 뒤 근무 시간을 등록해 주세요.");
            }
            schedule.changeWorkHours(start, end);  // 변경 감지로 저장
            return DriverWorkHoursResponseDTO.from(schedule);
        }

        DriverSchedule saved = scheduleRepository.save(DriverSchedule.builder()
                .driver(driver)
                .workDate(date)
                .startTime(start)
                .endTime(end)
                .status(DriverScheduleStatus.AVAILABLE)
                .build());
        return DriverWorkHoursResponseDTO.from(saved);
    }

    // 근무 가능 시간 삭제 — 휴무일은 휴무 취소(cancelOffDay)를 쓴다
    @Transactional
    @Override
    public void deleteWorkHours(Long memberId, LocalDate date) {
        Long driverId = getDriverId(memberId);

        if (date == null || date.isBefore(LocalDate.now())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "지난 날짜의 근무 시간은 지울 수 없습니다.");
        }

        DriverSchedule schedule = scheduleRepository
                .findByDriver_IdAndWorkDate(driverId, date)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "등록된 근무 시간이 없습니다."));

        if (schedule.getStatus() != DriverScheduleStatus.AVAILABLE) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "휴무일은 휴무 취소로 지워 주세요.");
        }

        scheduleRepository.delete(schedule);
    }
}
