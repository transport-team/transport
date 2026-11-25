package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Driver;
import com.transport.domain.Settlement;
import com.transport.domain.enums.SettlementStatus;
import com.transport.dto.DriverCompletedShipmentDTO;
import com.transport.dto.DriverCompletedTotalDTO;
import com.transport.dto.DriverReviewSummaryResponseDTO;
import com.transport.dto.ReviewResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;
import com.transport.repository.SettlementPeriodRepository;
import com.transport.repository.SettlementRepository;

// 기사 운임 조회 서비스: 날짜 계산, DTO 매핑, 입력값 검증 (DB 연결 없이 확인)
@ExtendWith(MockitoExtension.class)
class DriverSettlementServiceTest {

    @Mock
    private StatusHistoryService statusHistoryService;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ReviewService reviewService;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SettlementPeriodRepository periodRepository;

    @InjectMocks
    private DriverSettlementServiceImpl service;

    // ===== 내 평점 · 리뷰 (DRV-07) =====

    @Test
    void myReviewsUseDriverIdNotMemberId() {
        // 회원 7번 = 기사 40번 → 리뷰는 기사 ID로 찾는다 (collaboration.md: getAverageRating의 driverId는 driver.id)
        when(driverRepository.findByMemberId(7L)).thenReturn(Optional.of(Driver.builder().id(40L).build()));
        List<ReviewResponseDTO> reviews = List.of(
                ReviewResponseDTO.builder().id(2L).rating(5).customerName("김*수").content("친절해요").build(),
                ReviewResponseDTO.builder().id(1L).rating(3).customerName("이*").build());
        when(reviewService.getDriverReviews(40L)).thenReturn(reviews);
        when(reviewService.getAverageRating(40L)).thenReturn(4.0);

        DriverReviewSummaryResponseDTO result = service.getMyReviews(7L);

        assertThat(result.averageRating()).isEqualTo(4.0);
        assertThat(result.reviewCount()).isEqualTo(2);
        assertThat(result.reviews()).extracting(ReviewResponseDTO::getCustomerName).containsExactly("김*수", "이*");
        verifyNoInteractions(statusHistoryService);
    }

    @Test
    void myReviewsWithoutReviewsAreZero() {
        when(driverRepository.findByMemberId(7L)).thenReturn(Optional.of(Driver.builder().id(40L).build()));
        when(reviewService.getDriverReviews(40L)).thenReturn(List.of());
        when(reviewService.getAverageRating(40L)).thenReturn(0.0);

        DriverReviewSummaryResponseDTO result = service.getMyReviews(7L);

        assertThat(result.averageRating()).isZero();
        assertThat(result.reviewCount()).isZero();
        assertThat(result.reviews()).isEmpty();
    }

    @Test
    void myReviewsOfUnknownDriverIsNotFound() {
        when(driverRepository.findByMemberId(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getMyReviews(99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.NOT_FOUND);
        verifyNoInteractions(reviewService);
    }

    @Test
    void monthlySummaryUsesFirstDayAndNextMonthFirstDay() {
        var total = new DriverCompletedTotalDTO(2L, 150_000L);
        when(statusHistoryService.getDriverCompletedTotal(
                7L,
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0)))
                .thenReturn(total);

        var result = service.getMonthlySummary(7L, "2026-11");

        assertThat(result.settleMonth()).isEqualTo("2026-11");
        assertThat(result.completedCount()).isEqualTo(2L);
        assertThat(result.totalFare()).isEqualTo(150_000L);
        // 기사 정보 · 마감 기록이 없으면 마감 전, 금액은 비운다
        assertThat(result.settlementStatus()).isEqualTo("OPEN");
        assertThat(result.payoutAmount()).isNull();
    }

    // ===== 그 달 정산 상태 (마감 전 · 지급 대기 · 지급 완료 · 정산 없음) =====

    private void stubOctober(long count, long fare) {
        var total = new DriverCompletedTotalDTO(count, fare);
        when(statusHistoryService.getDriverCompletedTotal(
                7L,
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 11, 1, 0, 0)))
                .thenReturn(total);
        when(driverRepository.findByMemberId(7L)).thenReturn(Optional.of(Driver.builder().id(40L).build()));
    }

    private Settlement october(SettlementStatus status, LocalDateTime paidAt) {
        return Settlement.builder()
                .id(5L).settleMonth("2026-10")
                .completedCount(3).totalFare(150_000)
                .commissionRate(10).payoutAmount(135_000)
                .status(status).paidAt(paidAt)
                .build();
    }

    @Test
    void closedMonthWithPendingSettlementShowsPayout() {
        stubOctober(3, 150_000);
        when(settlementRepository.findByDriver_IdAndSettleMonth(40L, "2026-10"))
                .thenReturn(Optional.of(october(SettlementStatus.PENDING, null)));
        when(periodRepository.existsById("2026-10")).thenReturn(true);

        var result = service.getMonthlySummary(7L, "2026-10");

        assertThat(result.settlementStatus()).isEqualTo("PENDING");
        assertThat(result.commissionRate()).isEqualTo(10);
        assertThat(result.payoutAmount()).isEqualTo(135_000);
        assertThat(result.paidAt()).isNull();
    }

    @Test
    void paidSettlementShowsPaidAt() {
        LocalDateTime paidAt = LocalDateTime.of(2026, 11, 5, 10, 0);
        stubOctober(3, 150_000);
        when(settlementRepository.findByDriver_IdAndSettleMonth(40L, "2026-10"))
                .thenReturn(Optional.of(october(SettlementStatus.PAID, paidAt)));
        when(periodRepository.existsById("2026-10")).thenReturn(true);

        var result = service.getMonthlySummary(7L, "2026-10");

        assertThat(result.settlementStatus()).isEqualTo("PAID");
        assertThat(result.payoutAmount()).isEqualTo(135_000);
        assertThat(result.paidAt()).isEqualTo(paidAt);
    }

    @Test
    void closedMonthWithoutSettlementIsNone() {
        // 마감 때 완료 실적이 0건인 기사는 정산 행을 만들지 않는다 (AdminSettlementServiceImpl.closeMonth)
        stubOctober(0, 0);
        when(settlementRepository.findByDriver_IdAndSettleMonth(40L, "2026-10")).thenReturn(Optional.empty());
        when(periodRepository.existsById("2026-10")).thenReturn(true);

        var result = service.getMonthlySummary(7L, "2026-10");

        assertThat(result.settlementStatus()).isEqualTo("NONE");
        assertThat(result.commissionRate()).isNull();
        assertThat(result.payoutAmount()).isNull();
        assertThat(result.paidAt()).isNull();
    }

    @Test
    void draftBeforeClosingIsOpenWithoutAmounts() {
        // 구버전 마감 전 초안(PENDING)은 금액이 바뀔 수 있어 마감 전으로 보여 주고 금액은 비운다
        stubOctober(3, 150_000);
        when(settlementRepository.findByDriver_IdAndSettleMonth(40L, "2026-10"))
                .thenReturn(Optional.of(october(SettlementStatus.PENDING, null)));
        when(periodRepository.existsById("2026-10")).thenReturn(false);

        var result = service.getMonthlySummary(7L, "2026-10");

        assertThat(result.settlementStatus()).isEqualTo("OPEN");
        assertThat(result.payoutAmount()).isNull();
        assertThat(result.completedCount()).isEqualTo(3L);
    }

    @Test
    void monthlyItemsMapsFareAndCompletionTime() {
        LocalDateTime completedAt = LocalDateTime.of(2026, 11, 19, 12, 30);
        var item = new DriverCompletedShipmentDTO(12L, "가전제품", 85_000, completedAt);
        when(statusHistoryService.getDriverCompletedShipments(
                7L,
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0)))
                .thenReturn(List.of(item));

        var results = service.getMonthlyItems(7L, "2026-11");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).shipmentId()).isEqualTo(12L);
        assertThat(results.get(0).cargoName()).isEqualTo("가전제품");
        assertThat(results.get(0).fare()).isEqualTo(85_000);
        assertThat(results.get(0).completedAt()).isEqualTo(completedAt);
    }

    @Test
    void rangeSummaryIncludesEndDate() {
        LocalDate from = LocalDate.of(2026, 11, 1);
        LocalDate to = LocalDate.of(2026, 11, 19);
        var total = new DriverCompletedTotalDTO(3L, 240_000L);
        when(statusHistoryService.getDriverCompletedTotal(
                7L,
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 11, 20, 0, 0)))
                .thenReturn(total);

        var result = service.getRangeSummary(7L, from, to);

        assertThat(result.from()).isEqualTo(from);
        assertThat(result.to()).isEqualTo(to);
        assertThat(result.completedCount()).isEqualTo(3L);
        assertThat(result.totalFare()).isEqualTo(240_000L);
    }

    @Test
    void singleDayRangeItemsUsesNextDayAsExclusiveBound() {
        LocalDate day = LocalDate.of(2026, 11, 19);
        when(statusHistoryService.getDriverCompletedShipments(
                7L,
                day.atStartOfDay(),
                day.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        assertThat(service.getRangeItems(7L, day, day)).isEmpty();
        verify(statusHistoryService).getDriverCompletedShipments(
                7L,
                day.atStartOfDay(),
                day.plusDays(1).atStartOfDay());
    }

    @Test
    void emptyRangeReturnsZeroTotals() {
        LocalDate day = LocalDate.of(2026, 11, 19);
        var total = new DriverCompletedTotalDTO(0L, 0L);
        when(statusHistoryService.getDriverCompletedTotal(
                7L,
                day.atStartOfDay(), day.plusDays(1).atStartOfDay()))
                .thenReturn(total);

        var result = service.getRangeSummary(7L, day, day);

        assertThat(result.completedCount()).isZero();
        assertThat(result.totalFare()).isZero();
    }

    @Test
    void rejectsInvalidMonthWithoutQueryingRepository() {
        assertThatThrownBy(() -> service.getMonthlySummary(7L, "2026-13"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.getMonthlyItems(7L, "2026-11-19"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.getMonthlySummary(null, "2026-11"))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.getMonthlyItems(7L, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(statusHistoryService);
    }

    @Test
    void rejectsInvalidRangeWithoutQueryingRepository() {
        LocalDate from = LocalDate.of(2026, 11, 20);
        LocalDate to = LocalDate.of(2026, 11, 19);
        assertThatThrownBy(() -> service.getRangeSummary(7L, from, to))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.getRangeItems(7L, from, to))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.getRangeSummary(null, to, to))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.getRangeItems(7L, null, to))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        assertThatThrownBy(() -> service.getRangeSummary(7L, to, null))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
        verifyNoInteractions(statusHistoryService);
    }
}
