package com.transport.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Driver;
import com.transport.domain.Settlement;
import com.transport.domain.enums.SettlementStatus;
import com.transport.dto.DriverMonthlySettlementResponseDTO;
import com.transport.dto.DriverReviewSummaryResponseDTO;
import com.transport.dto.DriverSettlementItemResponseDTO;
import com.transport.dto.DriverSettlementRangeResponseDTO;
import com.transport.dto.ReviewResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;
import com.transport.repository.SettlementPeriodRepository;
import com.transport.repository.SettlementRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DriverSettlementServiceImpl implements DriverSettlementService {

    private final StatusHistoryService statusHistoryService;  // 팀원 4 약속 메서드 (기사 운송완료 합계 · 내역)
    private final DriverRepository driverRepository;
    private final ReviewService reviewService;  // 팀원 4 약속 메서드 (평균 평점 · 리뷰 목록, driverId = driver.id)
    private final SettlementRepository settlementRepository;  // 월 정산 행 (지급 대기 · 지급 완료)
    private final SettlementPeriodRepository periodRepository;  // 월 마감 여부

    // 내 평균 평점 · 리뷰 (DRV-07) — 리뷰는 driver.id로 찾으니 로그인 회원의 기사 정보부터
    @Override
    public DriverReviewSummaryResponseDTO getMyReviews(Long memberId) {
        Driver driver = driverRepository.findByMemberId(memberId)
                .orElseThrow(() -> new CustomException(
                        ErrorCode.NOT_FOUND,
                        "기사 정보를 찾을 수 없습니다."));

        List<ReviewResponseDTO> reviews = reviewService.getDriverReviews(driver.getId());
        return new DriverReviewSummaryResponseDTO(
                reviewService.getAverageRating(driver.getId()),
                reviews.size(),
                reviews);
    }

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

        var total = statusHistoryService.getDriverCompletedTotal(
                memberId,
                from,
                to);

        String monthText = month.toString();
        Settlement stored = driverRepository.findByMemberId(memberId)
                .flatMap(driver -> settlementRepository
                        .findByDriver_IdAndSettleMonth(driver.getId(), monthText))
                .orElse(null);

        String state = settlementState(stored, periodRepository.existsById(monthText));
        // 마감 전 초안(구버전)의 금액은 바뀔 수 있으니 지급 대기 · 지급 완료일 때만 금액을 보낸다
        boolean settled = "PENDING".equals(state) || "PAID".equals(state);

        return new DriverMonthlySettlementResponseDTO(
                monthText,
                total.completedCount(),
                total.totalFare(),
                state,
                settled ? stored.getCommissionRate() : null,
                settled ? stored.getPayoutAmount() : null,
                settled ? stored.getPaidAt() : null);
    }

    // 그 달 정산 상태 — 마감 여부는 settlement_period, 지급 여부는 settlement 행으로 본다
    // (이전 버전에서 마감 없이 지급한 기록이 있으면 마감 전이어도 PAID)
    private String settlementState(Settlement stored, boolean closed) {
        if (stored != null && stored.getStatus() == SettlementStatus.PAID) return "PAID";
        if (!closed) return "OPEN";
        return stored == null ? "NONE" : "PENDING";
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

        return statusHistoryService.getDriverCompletedShipments(
                memberId,
                from,
                to).stream()
                .map(item -> new DriverSettlementItemResponseDTO(
                        item.shipmentId(),
                        item.cargoName(),
                        item.completedAt(),
                        item.fare()))
                .toList();
    }

    // 선택한 기간의 정산 요약 조회 (종료일 포함)
    @Override
    public DriverSettlementRangeResponseDTO getRangeSummary(
            Long memberId, LocalDate from, LocalDate to) {

        validateRange(memberId, from, to);

        var total = statusHistoryService.getDriverCompletedTotal(
                memberId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay());

        return new DriverSettlementRangeResponseDTO(
                from,
                to,
                total.completedCount(),
                total.totalFare());
    }

    // 선택한 기간의 운임 상세 내역 조회
    @Override
    public List<DriverSettlementItemResponseDTO> getRangeItems(
            Long memberId, LocalDate from, LocalDate to) {

        validateRange(memberId, from, to);

        return statusHistoryService.getDriverCompletedShipments(
                memberId,
                from.atStartOfDay(),
                to.plusDays(1).atStartOfDay()).stream()
                .map(item -> new DriverSettlementItemResponseDTO(
                        item.shipmentId(),
                        item.cargoName(),
                        item.completedAt(),
                        item.fare()))
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
