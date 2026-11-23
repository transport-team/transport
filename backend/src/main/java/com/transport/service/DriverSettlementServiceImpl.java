package com.transport.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverMonthlySettlementResponseDTO;
import com.transport.dto.DriverSettlementItemResponseDTO;
import com.transport.dto.DriverSettlementRangeResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentStatusHistoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverSettlementServiceImpl implements DriverSettlementService {

    private final ShipmentStatusHistoryRepository historyRepository;

    // 기사별 월간 배송 완료 건수 및 운임 합계 조회
    @Override
    public DriverMonthlySettlementResponseDTO getMonthlySummary(
            Long memberId, String settleMonth) {

        if (memberId == null || settleMonth == null) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "기사 정보와 정산월을 확인해 주세요.");
        }

        YearMonth month;

        try {
            month = YearMonth.parse(settleMonth);
        } catch (DateTimeParseException e) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "정산월은 yyyy-MM 형식이어야 합니다.");
        }

        var from = month.atDay(1).atStartOfDay();
        var to = month.plusMonths(1).atDay(1).atStartOfDay();

        var total = historyRepository.findDriverMonthlyTotal(
                memberId,
                ShipmentStatus.COMPLETED,
                from,
                to);

        return new DriverMonthlySettlementResponseDTO(
                month.toString(),
                total.getCompletedCount(),
                total.getTotalFare());
    }

    // 기사별 월간 운임 상세 내역 조회
    @Override
    public List<DriverSettlementItemResponseDTO> getMonthlyItems(
            Long memberId, String settleMonth) {

        if (memberId == null || settleMonth == null) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "기사 정보와 정산월을 확인해 주세요.");
        }

        YearMonth month;

        try {
            month = YearMonth.parse(settleMonth);
        } catch (DateTimeParseException e) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "정산월은 yyyy-MM 형식이어야 합니다.");
        }

        var from = month.atDay(1).atStartOfDay();
        var to = month.plusMonths(1).atDay(1).atStartOfDay();

        return historyRepository.findDriverMonthlyFareItems(
                memberId,
                ShipmentStatus.COMPLETED,
                from,
                to).stream()
                .map(item -> new DriverSettlementItemResponseDTO(
                        item.getShipmentId(),
                        item.getCargoName(),
                        item.getCompletedAt(),
                        item.getFare()))
                .toList();
    }

    // 선택한 기간의 정산 요약 조회 (종료일 포함)
    @Override
    public DriverSettlementRangeResponseDTO getRangeSummary(
            Long memberId, LocalDate from, LocalDate to) {

        validateRange(memberId, from, to);

        var total = historyRepository.findDriverMonthlyTotal(
                memberId,
                ShipmentStatus.COMPLETED,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());

        return new DriverSettlementRangeResponseDTO(
                from,
                to,
                total.getCompletedCount(),
                total.getTotalFare());
    }

    // 선택한 기간의 운임 상세 내역 조회
    @Override
    public List<DriverSettlementItemResponseDTO> getRangeItems(
            Long memberId, LocalDate from, LocalDate to) {

        validateRange(memberId, from, to);

        return historyRepository.findDriverMonthlyFareItems(
                memberId,
                ShipmentStatus.COMPLETED,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()).stream()
                .map(item -> new DriverSettlementItemResponseDTO(
                        item.getShipmentId(),
                        item.getCargoName(),
                        item.getCompletedAt(),
                        item.getFare()))
                .toList();
    }

    private void validateRange(Long memberId, LocalDate from, LocalDate to) {
        if (memberId == null || from == null || to == null
                || from.isAfter(to)) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "올바른 정산 조회 기간을 선택해 주세요.");
        }
    }

}
