package com.transport.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Driver;
import com.transport.domain.Settlement;
import com.transport.domain.SettlementItem;
import com.transport.domain.SettlementPeriod;
import com.transport.domain.enums.SettlementStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.AdminSettlementMonthResponseDTO;
import com.transport.dto.AdminSettlementDifferenceDTO;
import com.transport.dto.AdminSettlementTrendDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DriverRepository;
import com.transport.repository.SettlementItemRepository;
import com.transport.repository.SettlementPeriodRepository;
import com.transport.repository.SettlementRepository;
import com.transport.repository.SettlementFareRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminSettlementServiceImpl implements AdminSettlementService {
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    // 팀 정책: 모든 기사에게 동일한 10% 적용. 이미 마감한 정산의 비율은 보존한다.
    private static final int STANDARD_COMMISSION_RATE = 10;
    // 현재 화면의 '다음 달 5일' 지급 안내 기준. 실제 회사 정책은 팀 확인 필요.
    private static final int PAYMENT_START_DAY = 5;

    private final DriverRepository driverRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementPeriodRepository periodRepository;
    private final SettlementItemRepository itemRepository;
    private final SettlementFareRepository fareRepository;
    private final EntityManager entityManager;

    @Override
    public AdminSettlementMonthResponseDTO getMonthlySettlements(String month) {
        YearMonth target = parseMonth(month);
        SettlementPeriod period = periodRepository.findById(month).orElse(null);
        boolean closed = period != null;
        List<AdminSettlementMonthResponseDTO.Item> items = new ArrayList<>();

        // 기사 · 운송완료 운임 · 정산 행을 한 번씩만 읽고 기사별로 나눈다 (기사마다 3번씩 조회하던 N+1 정리, 11-25)
        Map<Long, Totals> completedByDriver = readCompletedByDriver(target);
        Map<Long, Settlement> storedByDriver = new HashMap<>();
        for (Settlement settlement : settlementRepository.findBySettleMonth(month)) {
            storedByDriver.putIfAbsent(settlement.getDriver().getId(), settlement);
        }

        for (Driver driver : driverRepository.findAllWithMember()) {
            Totals current = completedByDriver.getOrDefault(driver.getId(), EMPTY_TOTALS);
            Settlement stored = storedByDriver.get(driver.getId());
            // 운송 실적도 기존 정산 기록도 없는 기사는 목록에 넣지 않는다.
            if (current.count() == 0 && stored == null) continue;
            // 미리 계산해 두었던 초안은 마감 전에는 확정 금액으로 보여주지 않는다.
            items.add(toItem(driver, current, stored, STANDARD_COMMISSION_RATE, closed));
        }

        items.sort(Comparator.comparing(AdminSettlementMonthResponseDTO.Item::driverName)
                .thenComparing(AdminSettlementMonthResponseDTO.Item::driverId));
        long totalFare = items.stream().filter(item -> !"LATE".equals(item.status()))
                .mapToLong(AdminSettlementMonthResponseDTO.Item::totalFare).sum();
        long totalPayout = items.stream().filter(item -> item.payoutAmount() != null)
                .mapToLong(item -> item.payoutAmount()).sum();
        long pending = items.stream().filter(item -> "PENDING".equals(item.status())).count();
        long paid = items.stream().filter(item -> "PAID".equals(item.status())).count();
        long uncreated = items.stream().filter(item -> "NOT_CREATED".equals(item.status())).count();
        // 반올림은 기사별로 수행하므로, 수수료 합계는 운임 - 지급액으로 계산한다.
        long totalCommission = totalFare - totalPayout;
        return new AdminSettlementMonthResponseDTO(month, STANDARD_COMMISSION_RATE, closed,
                closed ? period.getClosedAt() : null, totalFare, totalCommission, totalPayout,
                pending, paid, uncreated, 0, items);
    }

    @Override
    public List<AdminSettlementTrendDTO> getMonthlyTrend(String endMonth) {
        YearMonth end = parseMonth(endMonth);
        List<AdminSettlementTrendDTO> trend = new ArrayList<>();
        for (int offset = 5; offset >= 0; offset--) {
            String month = end.minusMonths(offset).toString();
            AdminSettlementMonthResponseDTO summary = getMonthlySettlements(month);
            trend.add(new AdminSettlementTrendDTO(month, summary.totalFare(),
                    summary.totalCommission()));
        }
        return trend;
    }

    @Override
    @Transactional
    public AdminSettlementMonthResponseDTO closeMonth(String month) {
        YearMonth target = parseMonth(month);
        if (!target.isBefore(YearMonth.now(SEOUL))) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "이번 달은 아직 마감할 수 없습니다. 다음 달 1일부터 마감 가능합니다.");
        }
        if (periodRepository.existsById(month)) return getMonthlySettlements(month);

        // 먼저 기존 지급 기록/중복 배송을 검증한다. 하나라도 실패하면 월 전체를 마감하지 않는다.
        List<ClosePlan> plans = new ArrayList<>();
        for (Driver driver : driverRepository.findAll()) {
            Totals totals = readCompleted(driver, target);
            Settlement stored = settlementRepository
                    .findByDriver_IdAndSettleMonth(driver.getId(), month).orElse(null);
            if (totals.count() == 0 && stored == null) continue;

            if (stored != null && stored.getStatus() == SettlementStatus.PAID) {
                // 이전 버전의 수동 지급 내역은 금액을 변경하지 않는다.
                if (stored.getCompletedCount() != totals.count() || stored.getTotalFare() != totals.fare()) {
                    throw new CustomException(ErrorCode.INVALID_INPUT,
                            driver.getMember().getName() + " 기사의 기존 지급 기록과 완료 실적이 다릅니다. 별도 확인이 필요합니다.");
                }
                plans.add(new ClosePlan(driver, stored, totals, stored.getCommissionRate()));
                continue;
            }
            if (totals.count() == 0) {
                // 구버전에서 저장한 0건 초안도 마감 때 0원으로 정리한다.
                if (stored != null) {
                    plans.add(new ClosePlan(driver, stored, totals, STANDARD_COMMISSION_RATE));
                }
                continue;
            }
            plans.add(new ClosePlan(driver, stored, totals, STANDARD_COMMISSION_RATE));
        }

        for (ClosePlan plan : plans) {
            Settlement stored = plan.stored();
            Totals totals = plan.totals();
            if (stored == null) {
                stored = settlementRepository.save(Settlement.builder()
                        .driver(plan.driver())
                        .settleMonth(month)
                        .completedCount(toInt(totals.count()))
                        .totalFare(toInt(totals.fare()))
                        .commissionRate(plan.rate())
                        .payoutAmount(payout(totals.fare(), plan.rate()))
                        .status(SettlementStatus.PENDING)
                        .build());
            } else if (stored.getStatus() == SettlementStatus.PENDING) {
                entityManager.refresh(stored, LockModeType.PESSIMISTIC_WRITE);
                stored.refreshDraft(toInt(totals.count()), toInt(totals.fare()),
                        plan.rate(), payout(totals.fare(), plan.rate()));
            }
            // 기존 지급 완료 기록은 그대로 두고, 운송 단위 명세만 추가해 중복 집계를 막는다.
            for (FareLine line : totals.lines()) {
                if (itemRepository.existsByShipmentId(line.shipmentId())) {
                    throw new CustomException(ErrorCode.INVALID_INPUT,
                            "이미 다른 정산에 반영된 운송 건이 있습니다. 운송번호: " + line.shipmentId());
                }
                itemRepository.save(SettlementItem.builder()
                        .settlement(stored).shipmentId(line.shipmentId())
                        .fare(line.fare()).completedAt(line.completedAt()).build());
            }
        }
        // 해당 월 전체 기사 정산을 한 트랜잭션으로 확정한다.
        periodRepository.saveAndFlush(new SettlementPeriod(month, LocalDateTime.now(SEOUL)));
        return getMonthlySettlements(month);
    }

    @Override
    public AdminSettlementDifferenceDTO getDifferences(String month, Long driverId) {
        YearMonth target = parseMonth(month);
        if (!periodRepository.existsById(month)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "마감된 월만 실적 차이를 조회할 수 있습니다.");
        }
        Driver driver = driverRepository.findById(driverId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "기사를 찾을 수 없습니다."));
        Totals current = readCompleted(driver, target);
        Settlement stored = settlementRepository
                .findByDriver_IdAndSettleMonth(driverId, month).orElse(null);
        List<AdminSettlementDifferenceDTO.Line> differences = new ArrayList<>();

        if (stored == null) {
            for (FareLine line : current.lines()) {
                differences.add(new AdminSettlementDifferenceDTO.Line(
                        line.shipmentId(), null, line.fare(), "NOT_IN_CLOSED_SETTLEMENT"));
            }
            return new AdminSettlementDifferenceDTO(month, driverId,
                    driver.getMember().getName(), "마감 당시 정산에 포함되지 않은 현재 완료 운송입니다.", differences);
        }

        List<SettlementItem> frozen = itemRepository.findBySettlement_Id(stored.getId());
        if (frozen.isEmpty() && stored.getCompletedCount() > 0) {
            // 구버전 정산처럼 건별 명세가 없는 경우, 임의로 차이 운송번호를 지어내지 않는다.
            return new AdminSettlementDifferenceDTO(month, driverId, driver.getMember().getName(),
                    "과거 확정 정산에 건별 명세가 없어, 개별 운송 차이를 특정할 수 없습니다.", differences);
        }
        Map<Long, Integer> frozenByShipment = new LinkedHashMap<>();
        for (SettlementItem item : frozen) frozenByShipment.put(item.getShipmentId(), item.getFare());
        for (FareLine line : current.lines()) {
            Integer fixed = frozenByShipment.remove(line.shipmentId());
            if (fixed == null) {
                differences.add(new AdminSettlementDifferenceDTO.Line(
                        line.shipmentId(), null, line.fare(), "NOT_IN_CLOSED_SETTLEMENT"));
            } else if (fixed.intValue() != line.fare()) {
                differences.add(new AdminSettlementDifferenceDTO.Line(
                        line.shipmentId(), fixed, line.fare(), "FARE_CHANGED"));
            }
        }
        frozenByShipment.forEach((shipmentId, fixedFare) -> differences.add(
                new AdminSettlementDifferenceDTO.Line(shipmentId, fixedFare, null, "NOT_IN_CURRENT_COMPLETED")));
        return new AdminSettlementDifferenceDTO(month, driverId,
                driver.getMember().getName(), "차이 내역은 확인용이며 확정 금액은 변경되지 않습니다.", differences);
    }

    @Override
    @Transactional
    public AdminSettlementMonthResponseDTO.Item markPaid(Long settlementId, String adminEmail, String transferReference) {
        if (adminEmail == null || adminEmail.isBlank() || transferReference == null
                || transferReference.isBlank() || transferReference.length() > 100) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "송금 확인 정보가 올바르지 않습니다.");
        }
        if (settlementId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "정산 번호를 확인해 주세요.");
        }
        Settlement settlement = entityManager.find(Settlement.class, settlementId,
                LockModeType.PESSIMISTIC_WRITE);
        if (settlement == null) {
            throw new CustomException(ErrorCode.NOT_FOUND, "정산 내역을 찾을 수 없습니다.");
        }
        if (!periodRepository.existsById(settlement.getSettleMonth())) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "아직 월 정산이 마감되지 않았습니다. 마감 후 지급해 주세요.");
        }
        // 화면의 지급 예정일(익월 5일) 이전에는 지급 완료 기록을 차단한다.
        LocalDate earliest = YearMonth.parse(settlement.getSettleMonth())
                .plusMonths(1).atDay(PAYMENT_START_DAY);
        if (LocalDate.now(SEOUL).isBefore(earliest)) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "지급 예정일(" + earliest + ") 이전에는 지급 완료를 기록할 수 없습니다.");
        }
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "이미 지급 처리된 정산입니다.");
        }
        if (settlement.getCompletedCount() <= 0) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "배송 완료 실적이 없는 정산은 지급 처리할 수 없습니다.");
        }
        settlement.markPaid(LocalDateTime.now(SEOUL), adminEmail, transferReference.trim());
        return toItem(settlement.getDriver(),
                new Totals(settlement.getCompletedCount(), settlement.getTotalFare(), List.of()),
                settlement, settlement.getCommissionRate(), true);
    }

    private Totals readCompleted(Driver driver, YearMonth month) {
        var records = fareRepository.findCompletedForDriver(
                driver.getId(), ShipmentStatus.COMPLETED,
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        Map<Long, FareLine> unique = new LinkedHashMap<>();
        if (records != null) {
            for (var item : records) {
                addLine(unique, item.getShipmentId(), item.getFare(), item.getCompletedAt());
            }
        }
        return toTotals(unique);
    }

    // 모든 기사의 그 달 운송완료를 한 번에 읽어 기사별 합계로 (readCompleted와 같은 규칙: 배송 중복 제거 · 빈 값이면 오류)
    private Map<Long, Totals> readCompletedByDriver(YearMonth month) {
        var records = fareRepository.findCompletedForAllDrivers(ShipmentStatus.COMPLETED,
                month.atDay(1).atStartOfDay(), month.plusMonths(1).atDay(1).atStartOfDay());
        Map<Long, Map<Long, FareLine>> linesByDriver = new HashMap<>();
        if (records != null) {
            for (var item : records) {
                addLine(linesByDriver.computeIfAbsent(item.getDriverId(), id -> new LinkedHashMap<>()),
                        item.getShipmentId(), item.getFare(), item.getCompletedAt());
            }
        }
        Map<Long, Totals> totals = new HashMap<>();
        linesByDriver.forEach((driverId, unique) -> totals.put(driverId, toTotals(unique)));
        return totals;
    }

    private void addLine(Map<Long, FareLine> unique, Long shipmentId, Integer fare, LocalDateTime completedAt) {
        if (shipmentId == null || fare == null || completedAt == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "배송 완료 명세에 누락된 값이 있습니다.");
        }
        unique.putIfAbsent(shipmentId, new FareLine(shipmentId, fare, completedAt));
    }

    private Totals toTotals(Map<Long, FareLine> unique) {
        long sum = unique.values().stream().mapToLong(FareLine::fare).sum();
        return new Totals(unique.size(), sum, List.copyOf(unique.values()));
    }

    private AdminSettlementMonthResponseDTO.Item toItem(Driver driver, Totals current,
            Settlement stored, Integer configuredRate, boolean closed) {
        boolean paid = stored != null && stored.getStatus() == SettlementStatus.PAID;
        // 마감 이후 발견된 소급 실적은 확정액에 합치지 않고 별도 확인 대상으로 표시한다.
        if (closed && stored == null) {
            return new AdminSettlementMonthResponseDTO.Item(null, driver.getId(),
                    driver.getMember().getName(), driver.getMember().getEmail(),
                    current.count(), current.fare(), configuredRate, null, "LATE", null, true,
                    null, null);
        }
        if (closed || paid) {
            boolean changed = current.count() != stored.getCompletedCount()
                    || current.fare() != stored.getTotalFare();
            return new AdminSettlementMonthResponseDTO.Item(stored.getId(), driver.getId(),
                    driver.getMember().getName(), driver.getMember().getEmail(),
                    stored.getCompletedCount(), stored.getTotalFare(), stored.getCommissionRate(),
                    stored.getPayoutAmount(), stored.getStatus().name(), stored.getPaidAt(), changed,
                    stored.getPaidByAdminEmail(), stored.getTransferReference());
        }
        // 마감 전에는 결코 저장된 초안의 금액을 확정액처럼 표시하지 않는다.
        return new AdminSettlementMonthResponseDTO.Item(
                null, driver.getId(), driver.getMember().getName(), driver.getMember().getEmail(),
                current.count(), current.fare(), configuredRate,
                payout(current.fare(), configuredRate),
                "NOT_CREATED", null, false, null, null);
    }

    private YearMonth parseMonth(String month) {
        if (month == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "정산월을 선택해 주세요.");
        }
        try {
            YearMonth parsed = YearMonth.parse(month);
            if (parsed.isAfter(YearMonth.now(SEOUL))) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "미래 월의 정산은 조회할 수 없습니다.");
            }
            return parsed;
        } catch (DateTimeParseException ex) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "정산월은 yyyy-MM 형식이어야 합니다.");
        }
    }

    private int payout(long fare, int rate) {
        int total = toInt(fare);
        int commission = BigDecimal.valueOf(total).multiply(BigDecimal.valueOf(rate))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP).intValueExact();
        return total - commission;
    }

    private int toInt(long value) {
        try {
            return Math.toIntExact(value);
        } catch (ArithmeticException ex) {
            throw new CustomException(ErrorCode.INVALID_INPUT,
                    "정산 금액 또는 완료 건수가 저장 가능 범위를 초과했습니다.");
        }
    }

    private record FareLine(Long shipmentId, int fare, LocalDateTime completedAt) { }
    private record Totals(long count, long fare, List<FareLine> lines) { }
    private static final Totals EMPTY_TOTALS = new Totals(0, 0, List.of());
    private record ClosePlan(Driver driver, Settlement stored, Totals totals, int rate) { }
}
