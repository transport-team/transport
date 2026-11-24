package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Settlement;
import com.transport.domain.SettlementPeriod;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.SettlementStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverSettlementItemResponseDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// 기사 정산 집계 JPQL을 실제 DB에서 검증한다 (Docker DB 필요, 테스트 종료 시 롤백)
@SpringBootTest
@Transactional
class DriverSettlementIntegrationTest {

    private static final LocalDate FROM = LocalDate.of(2001, 3, 1);
    private static final LocalDate TO = LocalDate.of(2001, 3, 31);

    @Autowired
    private DriverSettlementService settlementService;

    @PersistenceContext
    private EntityManager em;

    private Member customer;
    private Member driverMember;
    private Member otherDriverMember;
    private CargoType cargoType;
    private VehicleType vehicleType;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        customer = member("정산 고객", MemberRole.CUSTOMER);
        driverMember = driver("정산 기사");
        otherDriverMember = driver("다른 기사");
        cargoType = persist(CargoType.builder().name("정산확인" + suffix).active(true).build());
        vehicleType = persist(VehicleType.builder().code("SET_" + suffix)
                .name("정산 확인 차량").active(true).build());
    }

    @Test
    void rangeTotalsAndItemsOnlyIncludeCurrentCompletedShipmentsForDriver() {
        // 포함: 시작일 0시 / 종료일 23:59:59
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                FROM.atStartOfDay(), 100_000, "첫날 운송");
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                TO.atTime(23, 59, 59), 20_000, "마지막날 운송");

        // 제외: 기간 밖, 다른 기사, 현재 완료 상태가 아닌 배송, 완료 이력이 아닌 건
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                FROM.minusDays(1).atTime(23, 59, 59), 90_000, "이전 달");
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                TO.plusDays(1).atStartOfDay(), 90_000, "다음 달");
        history(otherDriverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                FROM.plusDays(2).atStartOfDay(), 90_000, "다른 기사");
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.IN_TRANSIT,
                FROM.plusDays(3).atStartOfDay(), 90_000, "현재 운송 중");
        history(driverMember, ShipmentStatus.ARRIVED, ShipmentStatus.COMPLETED,
                FROM.plusDays(4).atStartOfDay(), 90_000, "도착 이력");
        history(customer, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                FROM.plusDays(5).atStartOfDay(), 90_000, "고객 변경 이력");
        em.flush();

        var summary = settlementService.getRangeSummary(driverMember.getId(), FROM, TO);
        var items = settlementService.getRangeItems(driverMember.getId(), FROM, TO);

        assertThat(summary.completedCount()).isEqualTo(2L);
        assertThat(summary.totalFare()).isEqualTo(120_000L);
        assertThat(items).extracting(DriverSettlementItemResponseDTO::cargoName)
                .containsExactly("마지막날 운송", "첫날 운송");
        assertThat(items).extracting(DriverSettlementItemResponseDTO::fare)
                .containsExactly(20_000, 100_000);
    }

    @Test
    void monthlySummaryAndEmptyMonthUseRealQuery() {
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                FROM.atTime(8, 0), 55_000, "3월 배송");
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                LocalDate.of(2001, 4, 1).atStartOfDay(), 40_000, "4월 배송");
        em.flush();

        var march = settlementService.getMonthlySummary(driverMember.getId(), "2001-03");
        var april = settlementService.getMonthlySummary(driverMember.getId(), "2001-04");
        var may = settlementService.getMonthlySummary(driverMember.getId(), "2001-05");

        assertThat(march.completedCount()).isEqualTo(1L);
        assertThat(march.totalFare()).isEqualTo(55_000L);
        assertThat(april.completedCount()).isEqualTo(1L);
        assertThat(april.totalFare()).isEqualTo(40_000L);
        assertThat(may.completedCount()).isZero();
        assertThat(may.totalFare()).isZero();
        assertThat(settlementService.getMonthlyItems(driverMember.getId(), "2001-05")).isEmpty();
    }

    @Test
    void monthlySummaryReadsSettlementStateFromRealTables() {
        // 3월: 마감 + 지급 완료 / 4월: 마감 전 / 5월: 마감했지만 실적 없음 (다른 기사는 3월도 정산 없음)
        Driver driver = em.createQuery("select d from Driver d where d.member = :member", Driver.class)
                .setParameter("member", driverMember)
                .getSingleResult();
        LocalDateTime paidAt = LocalDateTime.of(2001, 4, 10, 15, 0);
        history(driverMember, ShipmentStatus.COMPLETED, ShipmentStatus.COMPLETED,
                FROM.atTime(8, 0), 100_000, "3월 배송");
        persist(new SettlementPeriod("2001-03", LocalDateTime.of(2001, 4, 1, 9, 0)));
        persist(new SettlementPeriod("2001-05", LocalDateTime.of(2001, 6, 1, 9, 0)));
        persist(Settlement.builder().driver(driver).settleMonth("2001-03")
                .completedCount(1).totalFare(100_000).commissionRate(10).payoutAmount(90_000)
                .status(SettlementStatus.PAID).paidAt(paidAt).build());
        em.flush();

        var march = settlementService.getMonthlySummary(driverMember.getId(), "2001-03");
        var april = settlementService.getMonthlySummary(driverMember.getId(), "2001-04");
        var may = settlementService.getMonthlySummary(driverMember.getId(), "2001-05");
        var otherMarch = settlementService.getMonthlySummary(otherDriverMember.getId(), "2001-03");

        assertThat(march.settlementStatus()).isEqualTo("PAID");
        assertThat(march.payoutAmount()).isEqualTo(90_000);
        assertThat(march.commissionRate()).isEqualTo(10);
        assertThat(march.paidAt()).isEqualTo(paidAt);
        assertThat(april.settlementStatus()).isEqualTo("OPEN");
        assertThat(april.payoutAmount()).isNull();
        assertThat(may.settlementStatus()).isEqualTo("NONE");
        assertThat(otherMarch.settlementStatus()).isEqualTo("NONE");
        assertThat(otherMarch.payoutAmount()).isNull();
    }

    private void history(Member actor, ShipmentStatus toStatus, ShipmentStatus currentStatus,
            LocalDateTime changedAt, int fare, String cargoName) {
        Shipment shipment = persist(Shipment.builder()
                .customer(customer)
                .vehicleType(vehicleType)
                .cargoType(cargoType)
                .originAddress("서울특별시 중구 세종대로 110")
                .destAddress("경기도 수원시 팔달구 효원로 1")
                .cargoName(cargoName)
                .pickupAt(FROM.atTime(9, 0))
                .senderName("보내는 사람")
                .senderPhone("010-1111-2222")
                .receiverName("받는 사람")
                .receiverPhone("010-3333-4444")
                .fare(fare)
                .durationMin(60)
                .status(currentStatus)
                .build());

        persist(ShipmentStatusHistory.builder()
                .shipment(shipment)
                .fromStatus(ShipmentStatus.ARRIVED)
                .toStatus(toStatus)
                .changedBy(actor)
                .changedAt(changedAt)
                .build());
    }

    private Member driver(String name) {
        Member member = member(name, MemberRole.DRIVER);
        persist(Driver.builder().member(member).licenseType("1종 보통")
                .careerYears(3).status(DriverStatus.ACTIVE).build());
        return member;
    }

    private Member member(String name, MemberRole role) {
        return persist(Member.builder()
                .email("settlement-" + UUID.randomUUID() + "@test.com")
                .password("x")
                .name(name)
                .phone("010-0000-0000")
                .role(role)
                .enabled(true)
                .build());
    }

    private <T> T persist(T entity) {
        em.persist(entity);
        return entity;
    }
}
