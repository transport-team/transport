package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentStatusHistoryRepository;

// 기사 운임 조회 서비스: 날짜 계산, DTO 매핑, 입력값 검증 (DB 연결 없이 확인)
@ExtendWith(MockitoExtension.class)
class DriverSettlementServiceTest {

    @Mock
    private ShipmentStatusHistoryRepository historyRepository;

    @InjectMocks
    private DriverSettlementService service;

    @Test
    void monthlySummaryUsesFirstDayAndNextMonthFirstDay() {
        var total = mock(ShipmentStatusHistoryRepository.DriverMonthlyTotal.class);
        when(total.getCompletedCount()).thenReturn(2L);
        when(total.getTotalFare()).thenReturn(150_000L);
        when(historyRepository.findDriverMonthlyTotal(
                7L, ShipmentStatus.COMPLETED,
                LocalDateTime.of(2026, 11, 1, 0, 0),
                LocalDateTime.of(2026, 12, 1, 0, 0)))
                .thenReturn(total);

        var result = service.getMonthlySummary(7L, "2026-11");

        assertThat(result.settleMonth()).isEqualTo("2026-11");
        assertThat(result.completedCount()).isEqualTo(2L);
        assertThat(result.totalFare()).isEqualTo(150_000L);
    }

    @Test
    void monthlyItemsMapsFareAndCompletionTime() {
        var item = mock(ShipmentStatusHistoryRepository.DriverMonthlyFareItem.class);
        LocalDateTime completedAt = LocalDateTime.of(2026, 11, 19, 12, 30);
        when(item.getShipmentId()).thenReturn(12L);
        when(item.getCargoName()).thenReturn("가전제품");
        when(item.getFare()).thenReturn(85_000);
        when(item.getCompletedAt()).thenReturn(completedAt);
        when(historyRepository.findDriverMonthlyFareItems(
                7L, ShipmentStatus.COMPLETED,
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
        var total = mock(ShipmentStatusHistoryRepository.DriverMonthlyTotal.class);
        when(total.getCompletedCount()).thenReturn(3L);
        when(total.getTotalFare()).thenReturn(240_000L);
        when(historyRepository.findDriverMonthlyTotal(
                7L, ShipmentStatus.COMPLETED,
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
        when(historyRepository.findDriverMonthlyFareItems(
                7L, ShipmentStatus.COMPLETED,
                day.atStartOfDay(),
                day.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        assertThat(service.getRangeItems(7L, day, day)).isEmpty();
        verify(historyRepository).findDriverMonthlyFareItems(
                7L, ShipmentStatus.COMPLETED,
                day.atStartOfDay(),
                day.plusDays(1).atStartOfDay());
    }

    @Test
    void emptyRangeReturnsZeroTotals() {
        LocalDate day = LocalDate.of(2026, 11, 19);
        var total = mock(ShipmentStatusHistoryRepository.DriverMonthlyTotal.class);
        when(total.getCompletedCount()).thenReturn(0L);
        when(total.getTotalFare()).thenReturn(0L);
        when(historyRepository.findDriverMonthlyTotal(
                7L, ShipmentStatus.COMPLETED,
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
        verifyNoInteractions(historyRepository);
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
        verifyNoInteractions(historyRepository);
    }
}
