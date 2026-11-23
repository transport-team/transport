package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Settlement;
import com.transport.domain.SettlementItem;
import com.transport.domain.SettlementPeriod;
import com.transport.domain.enums.SettlementStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.exception.CustomException;
import com.transport.repository.DriverRepository;
import com.transport.repository.SettlementItemRepository;
import com.transport.repository.SettlementPeriodRepository;
import com.transport.repository.SettlementRepository;
import com.transport.repository.SettlementFareRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@ExtendWith(MockitoExtension.class)
class AdminSettlementServiceImplTest {

    @Mock DriverRepository driverRepository;
    @Mock SettlementRepository settlementRepository;
    @Mock SettlementPeriodRepository periodRepository;
    @Mock SettlementItemRepository itemRepository;
    @Mock SettlementFareRepository fareRepository;
    @Mock EntityManager entityManager;
    @InjectMocks AdminSettlementServiceImpl service;

    private final Driver driver = Driver.builder().id(3L)
            .member(Member.builder().id(5L).name("테스트기사")
                    .email("driver@example.test").build())
            .build();

    private static SettlementFareRepository.SettlementFareItem item(long id, int fare) {
        return new SettlementFareRepository.SettlementFareItem() {
            @Override public Long getShipmentId() { return id; }
            @Override public Integer getFare() { return fare; }
            @Override public LocalDateTime getCompletedAt() { return LocalDateTime.of(2026, 10, 20, 12, 0); }
        };
    }

    private void withCompletedHistory() {
        when(fareRepository.findCompletedForDriver(eq(3L), eq(ShipmentStatus.COMPLETED),
                any(), any())).thenReturn(List.of(item(11, 50_000), item(12, 50_005)));
    }

    @Test
    void openMonthPreviewUsesCommonTenPercentWithoutAnyDriverRateSettings() {
        String month = YearMonth.now().toString();
        when(driverRepository.findAll()).thenReturn(List.of(driver));
        withCompletedHistory();
        var result = service.getMonthlySettlements(month);
        assertThat(result.standardCommissionRate()).isEqualTo(10);
        assertThat(result.totalFare()).isEqualTo(100_005);
        assertThat(result.totalCommission()).isEqualTo(10_001);
        assertThat(result.totalPayout()).isEqualTo(90_004);
        assertThat(result.missingRateCount()).isZero();
        assertThat(result.items().get(0).commissionRate()).isEqualTo(10);
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void currentMonthCannotClose() {
        assertThatThrownBy(() -> service.closeMonth(YearMonth.now().toString()))
                .isInstanceOf(CustomException.class);
        verify(periodRepository, never()).saveAndFlush(any());
    }

    @Test
    void closeMonthRequiresNoPerDriverSetupAndAutomaticallyUsesTenPercent() {
        String month = YearMonth.now().minusMonths(1).toString();
        when(driverRepository.findAll()).thenReturn(List.of(driver));
        withCompletedHistory();
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(call -> call.getArgument(0));

        service.closeMonth(month);
        ArgumentCaptor<Settlement> captured = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captured.capture());
        assertThat(captured.getValue().getCommissionRate()).isEqualTo(10);
        assertThat(captured.getValue().getPayoutAmount()).isEqualTo(90_004);
        verify(periodRepository).saveAndFlush(any(SettlementPeriod.class));
    }

    @Test
    void closeMonthFreezesCommonRateAndShipmentItems() {
        String month = YearMonth.now().minusMonths(1).toString();
        when(driverRepository.findAll()).thenReturn(List.of(driver));
        withCompletedHistory();
        when(settlementRepository.findByDriver_IdAndSettleMonth(3L, month))
                .thenReturn(Optional.empty());
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(call -> call.getArgument(0));

        service.closeMonth(month);
        ArgumentCaptor<Settlement> captured = ArgumentCaptor.forClass(Settlement.class);
        verify(settlementRepository).save(captured.capture());
        Settlement saved = captured.getValue();
        assertThat(saved.getStatus()).isEqualTo(SettlementStatus.PENDING);
        assertThat(saved.getCompletedCount()).isEqualTo(2);
        assertThat(saved.getTotalFare()).isEqualTo(100_005);
        assertThat(saved.getPayoutAmount()).isEqualTo(90_004);
        assertThat(saved.getCommissionRate()).isEqualTo(10);
        ArgumentCaptor<SettlementItem> lines = ArgumentCaptor.forClass(SettlementItem.class);
        verify(itemRepository, org.mockito.Mockito.times(2)).save(lines.capture());
        assertThat(lines.getAllValues()).extracting(SettlementItem::getShipmentId).containsExactly(11L, 12L);
        verify(periodRepository).saveAndFlush(any(SettlementPeriod.class));
    }

    @Test
    void legacyPendingDraftIsRecomputedUsingCommonTenPercentWhenMonthCloses() {
        String month = YearMonth.now().minusMonths(1).toString();
        when(driverRepository.findAll()).thenReturn(List.of(driver));
        withCompletedHistory();
        Settlement previous = Settlement.builder().id(9L).driver(driver).settleMonth(month)
                .completedCount(1).totalFare(50_000).commissionRate(5)
                .payoutAmount(47_500).status(SettlementStatus.PENDING).build();
        when(settlementRepository.findByDriver_IdAndSettleMonth(3L, month))
                .thenReturn(Optional.of(previous));

        service.closeMonth(month);
        assertThat(previous.getCompletedCount()).isEqualTo(2);
        assertThat(previous.getTotalFare()).isEqualTo(100_005);
        assertThat(previous.getCommissionRate()).isEqualTo(10);
        assertThat(previous.getPayoutAmount()).isEqualTo(90_004);
        verify(settlementRepository, never()).save(any());
        verify(periodRepository).saveAndFlush(any(SettlementPeriod.class));
    }

    @Test
    void duplicateShipmentCannotBeSettledAgain() {
        String month = YearMonth.now().minusMonths(1).toString();
        when(driverRepository.findAll()).thenReturn(List.of(driver));
        withCompletedHistory();
        when(settlementRepository.findByDriver_IdAndSettleMonth(3L, month))
                .thenReturn(Optional.empty());
        when(settlementRepository.save(any(Settlement.class))).thenAnswer(call -> call.getArgument(0));
        when(itemRepository.existsByShipmentId(11L)).thenReturn(true);

        assertThatThrownBy(() -> service.closeMonth(month)).isInstanceOf(CustomException.class);
        verify(periodRepository, never()).saveAndFlush(any());
    }

    @Test
    void paidMonthCannotBeReopenedAndMoneyIsNotRecalculated() {
        String month = YearMonth.now().minusMonths(1).toString();
        when(periodRepository.existsById(month)).thenReturn(true);
        when(periodRepository.findById(month))
                .thenReturn(Optional.of(new SettlementPeriod(month, LocalDateTime.now())));
        when(driverRepository.findAll()).thenReturn(List.of(driver));
        withCompletedHistory();
        Settlement paid = Settlement.builder().id(8L).driver(driver).settleMonth(month)
                .completedCount(1).totalFare(50_000).commissionRate(8)
                .payoutAmount(46_000).status(SettlementStatus.PAID).build();
        when(settlementRepository.findByDriver_IdAndSettleMonth(3L, month)).thenReturn(Optional.of(paid));

        var result = service.closeMonth(month);
        assertThat(result.closed()).isTrue();
        assertThat(result.items().get(0).payoutAmount()).isEqualTo(46_000);
        assertThat(result.items().get(0).commissionRate()).isEqualTo(8);
        assertThat(result.items().get(0).changedSinceDraft()).isTrue();
        verify(settlementRepository, never()).save(any());
    }

    @Test
    void trendReturnsSixConsecutiveMonthsInChronologicalOrder() {
        YearMonth end = YearMonth.now();
        var trend = service.getMonthlyTrend(end.toString());
        assertThat(trend).hasSize(6);
        assertThat(trend.get(0).month()).isEqualTo(end.minusMonths(5).toString());
        assertThat(trend.get(5).month()).isEqualTo(end.toString());
        assertThat(trend).allSatisfy(point -> {
            assertThat(point.totalFare()).isZero();
            assertThat(point.totalCommission()).isZero();
        });
    }

    @Test
    void paymentRequiresMonthToBeClosed() {
        String month = YearMonth.now().minusMonths(1).toString();
        Settlement pending = Settlement.builder().id(8L).driver(driver).settleMonth(month)
                .completedCount(2).totalFare(100_005).commissionRate(10)
                .payoutAmount(90_004).status(SettlementStatus.PENDING).build();
        when(entityManager.find(Settlement.class, 8L, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(pending);
        assertThatThrownBy(() -> service.markPaid(8L, "admin@transport.test", "BANK-TRANSFER-123")).isInstanceOf(CustomException.class);
        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    void paymentIsRecordedOnlyOnceAfterClose() {
        String month = YearMonth.now().minusMonths(2).toString();
        Settlement pending = Settlement.builder().id(8L).driver(driver).settleMonth(month)
                .completedCount(2).totalFare(100_005).commissionRate(10)
                .payoutAmount(90_004).status(SettlementStatus.PENDING).build();
        when(entityManager.find(Settlement.class, 8L, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(pending);
        when(periodRepository.existsById(month)).thenReturn(true);
        service.markPaid(8L, "admin@transport.test", "BANK-TRANSFER-123");
        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.PAID);
        assertThat(pending.getPaidAt()).isNotNull();
        assertThat(pending.getPaidByAdminEmail()).isEqualTo("admin@transport.test");
        assertThat(pending.getTransferReference()).isEqualTo("BANK-TRANSFER-123");
        assertThatThrownBy(() -> service.markPaid(8L, "admin@transport.test", "BANK-TRANSFER-123")).isInstanceOf(CustomException.class);
    }
    @Test
    void cannotRecordPaymentBeforeScheduledFifth() {
        String month = YearMonth.now().toString();
        Settlement pending = Settlement.builder().id(9L).driver(driver).settleMonth(month)
                .completedCount(1).totalFare(50_000).commissionRate(10)
                .payoutAmount(45_000).status(SettlementStatus.PENDING).build();
        when(entityManager.find(Settlement.class, 9L, LockModeType.PESSIMISTIC_WRITE))
                .thenReturn(pending);
        when(periodRepository.existsById(month)).thenReturn(true);
        assertThatThrownBy(() -> service.markPaid(9L, "admin@transport.test", "BANK-TRANSFER-XYZ"))
                .isInstanceOf(CustomException.class);
        assertThat(pending.getStatus()).isEqualTo(SettlementStatus.PENDING);
    }

    @Test
    void differencesShowAdditionalCompletedShipmentWithoutChangingFrozenSettlement() {
        String month = YearMonth.now().minusMonths(1).toString();
        Settlement frozen = Settlement.builder().id(42L).driver(driver).settleMonth(month)
                .completedCount(1).totalFare(50_000).commissionRate(10)
                .payoutAmount(45_000).status(SettlementStatus.PENDING).build();
        when(periodRepository.existsById(month)).thenReturn(true);
        when(driverRepository.findById(3L)).thenReturn(Optional.of(driver));
        when(settlementRepository.findByDriver_IdAndSettleMonth(3L, month))
                .thenReturn(Optional.of(frozen));
        withCompletedHistory();
        when(itemRepository.findBySettlement_Id(42L))
                .thenReturn(List.of(SettlementItem.builder().shipmentId(11L).fare(50_000)
                        .settlement(frozen).build()));

        var details = service.getDifferences(month, 3L);
        assertThat(details.lines()).hasSize(1);
        assertThat(details.lines().get(0).shipmentId()).isEqualTo(12L);
        assertThat(details.lines().get(0).differenceType()).isEqualTo("NOT_IN_CLOSED_SETTLEMENT");
        assertThat(frozen.getPayoutAmount()).isEqualTo(45_000);
        verify(settlementRepository, never()).save(any());
    }

}
