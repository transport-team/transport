package com.transport.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.DateTimeException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.DriverSchedule;
import com.transport.domain.Shipment;
import com.transport.domain.enums.DispatchApplicationStatus;
import com.transport.domain.enums.DriverScheduleStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchApplicationRepository;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.DriverScheduleRepository;
import com.transport.repository.ShipmentStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

// 기사 휴무일 관리
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverDayOffService {

    private final DriverRepository driverRepository;
    private final DriverScheduleRepository scheduleRepository;
    private final DispatchRepository dispatchRepository;
    private final DispatchApplicationRepository applicationRepository;
    private final ShipmentStatusHistoryRepository statusHistoryRepository;

    // 로그인 회원 ID로 기사 ID 조회
    private Long getDriverId(Long memberId) {
        return driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "기사 정보를 찾을 수 없습니다."))
                .getId();
    }

    // 월별 휴무일 조회
    public List<LocalDate> getOffDays(Long memberId, int year, int month) {
        Long driverId = getDriverId(memberId);

        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "조회할 연도 또는 월이 올바르지 않습니다.");
        }

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
                    "이미 등록된 근무 일정이 있는 날짜입니다.");
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
                .anyMatch(application -> statusHistoryRepository
                        .findFirstByShipment_IdAndToStatusOrderByChangedAtDesc(
                                application.getShipment().getId(),
                                ShipmentStatus.COMPLETED)
                        .map(history -> history.getChangedAt().toLocalDate().equals(date))
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
}
