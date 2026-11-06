package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.domain.Member;
import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DailyStatDTO;
import com.transport.dto.PaidStatDTO;
import com.transport.dto.VehicleTypeStatDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// 관리자 대시보드 집계 메서드 (배송 · 결제): 쿼리를 실제 MariaDB에서 확인한다 (Docker 필요)
// 테스트마다 데이터를 넣고 끝나면 롤백한다. 다른 데이터와 섞이지 않게 먼 과거 날짜를 쓴다
@SpringBootTest
@Transactional
class ShipmentPaymentStatsTest {

	private static final LocalDate DAY1 = LocalDate.of(2001, 1, 31);
	private static final LocalDate DAY2 = LocalDate.of(2001, 2, 1);

	@Autowired
	private PaymentService paymentService;

	@Autowired
	private ShipmentService shipmentService;

	@PersistenceContext
	private EntityManager em;

	private Member customer;
	private CargoType cargoType;
	private VehicleType small;
	private VehicleType truck;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		customer = persist(Member.builder().email("stats-" + suffix + "@test.com").password("x").name("통계 확인")
				.phone("010-0000-0000").role(MemberRole.CUSTOMER).enabled(true).build());
		cargoType = persist(CargoType.builder().name("통계" + suffix).active(true).build());
		small = persist(VehicleType.builder().code("STAT_S_" + suffix).name("통계 소형").active(true).build());
		truck = persist(VehicleType.builder().code("STAT_T_" + suffix).name("통계 1톤").active(true).build());
	}

	// 결제 완료(PAID)만, paidAt이 [from, to) 안인 것만 센다 — 환불 · 결제 전 · 기간 밖은 빠진다
	@Test
	void paidStat() {
		savePayments();

		PaidStatDTO twoDays = paymentService.getPaidStat(DAY1.atStartOfDay(), DAY2.plusDays(1).atStartOfDay());
		assertThat(twoDays.getCount()).isEqualTo(3);
		assertThat(twoDays.getRevenue()).isEqualTo(60_000);

		// 0시 0분 0초 결제는 그날로, 23시 59분 59초 결제는 전날로
		PaidStatDTO day2 = paymentService.getPaidStat(DAY2.atStartOfDay(), DAY2.plusDays(1).atStartOfDay());
		assertThat(day2.getCount()).isEqualTo(2);
		assertThat(day2.getRevenue()).isEqualTo(50_000);
	}

	@Test
	void paidStatWithoutPayments() {
		PaidStatDTO empty = paymentService.getPaidStat(LocalDate.of(1990, 1, 1).atStartOfDay(), LocalDate.of(1990, 1, 2).atStartOfDay());

		assertThat(empty.getCount()).isZero();
		assertThat(empty.getRevenue()).isZero();
	}

	// 날짜별: 결제가 있는 날만, 날짜 순 (toDate 하루 전체 포함)
	@Test
	void dailyPaidStats() {
		savePayments();

		List<DailyStatDTO> daily = paymentService.getDailyPaidStats(DAY1, DAY2);

		assertThat(daily).extracting(DailyStatDTO::getDate, DailyStatDTO::getCount, DailyStatDTO::getRevenue)
				.containsExactly(tuple(DAY1, 1L, 10_000L), tuple(DAY2, 2L, 50_000L));
	}

	// 차종별: 배송의 요청 차종으로 묶고 차종 id 순
	@Test
	void paidStatsByVehicleType() {
		savePayments();

		List<VehicleTypeStatDTO> stats = paymentService.getPaidStatsByVehicleType(DAY1.atStartOfDay(), DAY2.plusDays(1).atStartOfDay());

		assertThat(stats).extracting(VehicleTypeStatDTO::getVehicleTypeId, VehicleTypeStatDTO::getVehicleTypeName,
						VehicleTypeStatDTO::getCount, VehicleTypeStatDTO::getRevenue)
				.containsExactly(tuple(small.getId(), "통계 소형", 2L, 30_000L), tuple(truck.getId(), "통계 1톤", 1L, 30_000L));
	}

	// 상태별 건수: 모든 상태가 들어 있고, 넣은 만큼 늘어난다 (DB에 이미 있는 배송이 있어도 되게 차이로 확인)
	@Test
	void countByStatus() {
		Map<ShipmentStatus, Long> before = shipmentService.countByStatus();

		ship(small, ShipmentStatus.REQUESTED);
		ship(small, ShipmentStatus.REQUESTED);
		ship(truck, ShipmentStatus.IN_TRANSIT);
		em.flush();

		Map<ShipmentStatus, Long> after = shipmentService.countByStatus();
		assertThat(after).containsOnlyKeys(ShipmentStatus.values());
		assertThat(after.get(ShipmentStatus.REQUESTED) - before.get(ShipmentStatus.REQUESTED)).isEqualTo(2);
		assertThat(after.get(ShipmentStatus.IN_TRANSIT) - before.get(ShipmentStatus.IN_TRANSIT)).isEqualTo(1);
		assertThat(after.get(ShipmentStatus.COMPLETED)).isEqualTo(before.get(ShipmentStatus.COMPLETED));
	}

	// DAY1 · DAY2 결제 완료 3건(60,000원) + 빠져야 하는 3건
	private void savePayments() {
		pay(small, PaymentStatus.PAID, 10_000, DAY1.atTime(23, 59, 59));
		pay(small, PaymentStatus.PAID, 20_000, DAY2.atStartOfDay());
		pay(truck, PaymentStatus.PAID, 30_000, DAY2.atTime(13, 0));
		pay(truck, PaymentStatus.CANCELED, 99_000, DAY2.atTime(10, 0));          // 환불
		pay(truck, PaymentStatus.READY, 50_000, null);                            // 결제 전
		pay(small, PaymentStatus.PAID, 70_000, DAY2.plusDays(1).atStartOfDay());  // 기간 밖 (다음 날 0시)
		em.flush();
	}

	private void pay(VehicleType vehicleType, PaymentStatus status, int amount, LocalDateTime paidAt) {
		ShipmentStatus shipmentStatus = switch (status) {
			case PAID -> ShipmentStatus.REQUESTED;
			case READY -> ShipmentStatus.PENDING_PAYMENT;
			default -> ShipmentStatus.CANCELED;
		};
		persist(Payment.builder().shipment(ship(vehicleType, shipmentStatus)).orderId(UUID.randomUUID().toString())
				.amount(amount).status(status).paidAt(paidAt).build());
	}

	private Shipment ship(VehicleType vehicleType, ShipmentStatus status) {
		return persist(Shipment.builder().customer(customer).vehicleType(vehicleType).cargoType(cargoType)
				.originAddress("서울특별시 중구 세종대로 110").destAddress("경기도 수원시 팔달구 효원로 1")
				.cargoName("[통계 확인] 상자").pickupAt(DAY1.atTime(10, 0))
				.receiverName("확인용").receiverPhone("010-0000-0000").fare(10_000).status(status).build());
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}
}
