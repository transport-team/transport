package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CargoType;
import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Review;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverCompletedStatDTO;
import com.transport.dto.RatingStatDTO;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

// 관리자 대시보드 집계 메서드 (상태 이력 · 리뷰): 쿼리를 실제 MariaDB에서 확인한다 (Docker 필요)
// 테스트마다 데이터를 넣고 끝나면 롤백한다. 다른 데이터와 섞이지 않게 먼 과거 날짜를 쓴다
@SpringBootTest
@Transactional
class StatusHistoryReviewStatsTest {

	private static final LocalDateTime FROM = LocalDate.of(2001, 3, 1).atStartOfDay();
	private static final LocalDateTime TO = LocalDate.of(2001, 4, 1).atStartOfDay();

	@Autowired
	private StatusHistoryService statusHistoryService;

	@Autowired
	private ReviewService reviewService;

	@PersistenceContext
	private EntityManager em;

	private String suffix;
	private Member customer;
	private CargoType cargoType;
	private VehicleType vehicleType;

	@BeforeEach
	void setUp() {
		suffix = UUID.randomUUID().toString().substring(0, 8);
		customer = member("이력 통계 고객", MemberRole.CUSTOMER);
		cargoType = persist(CargoType.builder().name("이력통계" + suffix).active(true).build());
		vehicleType = persist(VehicleType.builder().code("HIST_" + suffix).name("이력 통계 1톤").active(true).build());
	}

	// 배송완료(COMPLETED)로 바뀐 것만, changedAt이 [from, to) 안인 것만 센다
	@Test
	void countCompleted() {
		Driver driver = driver("이력 기사");
		history(driver, ShipmentStatus.COMPLETED, FROM);                  // 시작 0시 — 들어간다
		history(driver, ShipmentStatus.COMPLETED, TO.minusSeconds(1));    // 끝나기 1초 전 — 들어간다
		history(driver, ShipmentStatus.IN_TRANSIT, FROM.plusDays(1));     // 완료가 아님
		history(driver, ShipmentStatus.COMPLETED, TO);                    // 다음 달 0시 — 빠진다
		history(driver, ShipmentStatus.COMPLETED, FROM.minusSeconds(1));  // 지난달 마지막 — 빠진다
		em.flush();

		assertThat(statusHistoryService.countCompleted(FROM, TO)).isEqualTo(2);
		assertThat(statusHistoryService.countCompleted(LocalDate.of(1990, 1, 1).atStartOfDay(), LocalDate.of(1990, 1, 2).atStartOfDay())).isZero();
	}

	// 기사별: 건수 많은 순, 같으면 기사 id 순, limit명까지 (기간 밖 · 완료가 아닌 이력은 빠진다)
	@Test
	void completedCountsByDriver() {
		Driver first = driver("순위 기사A");
		Driver second = driver("순위 기사B");
		Driver third = driver("순위 기사C");
		completed(third, 1);
		completed(second, 3);
		completed(first, 3);
		history(third, ShipmentStatus.COMPLETED, TO);                 // 기간 밖
		history(third, ShipmentStatus.ARRIVED, FROM.plusDays(2));     // 완료가 아님
		em.flush();

		List<DriverCompletedStatDTO> all = statusHistoryService.getCompletedCountsByDriver(FROM, TO, 5);
		assertThat(all)
				.extracting(DriverCompletedStatDTO::getDriverId, DriverCompletedStatDTO::getDriverName, DriverCompletedStatDTO::getCompletedCount)
				.containsExactly(tuple(first.getId(), "순위 기사A", 3L), tuple(second.getId(), "순위 기사B", 3L), tuple(third.getId(), "순위 기사C", 1L));

		assertThat(statusHistoryService.getCompletedCountsByDriver(FROM, TO, 2))
				.extracting(DriverCompletedStatDTO::getDriverId)
				.containsExactly(first.getId(), second.getId());
	}

	// 전체 리뷰 수 · 평균: 넣은 만큼 늘어나고, 평균은 DB의 평균을 소수 첫째 자리로 (DB에 이미 리뷰가 있어도 되게)
	@Test
	void ratingStat() {
		long before = reviewService.getRatingStat().getReviewCount();
		Driver driver = driver("평점 기사");
		review(driver, 5);
		review(driver, 4);
		review(driver, 4);
		em.flush();

		RatingStatDTO stat = reviewService.getRatingStat();
		Number dbAverage = (Number) em.createNativeQuery("select avg(rating) from review").getSingleResult();

		assertThat(stat.getReviewCount()).isEqualTo(before + 3);
		assertThat(stat.getAverageRating()).isEqualTo(Math.round(dbAverage.doubleValue() * 10.0) / 10.0);
		assertThat(reviewService.getAverageRating(driver.getId())).isEqualTo(4.3);  // 13 / 3
	}

	private void completed(Driver driver, int count) {
		for (int i = 0; i < count; i++) {
			history(driver, ShipmentStatus.COMPLETED, FROM.plusDays(i).plusHours(10));
		}
	}

	// 상태 이력 한 건: 배송마다 따로, 변경자는 기사 회원
	private void history(Driver driver, ShipmentStatus to, LocalDateTime changedAt) {
		persist(ShipmentStatusHistory.builder().shipment(ship(to)).fromStatus(ShipmentStatus.ARRIVED).toStatus(to)
				.changedBy(driver.getMember()).memo("이력 통계 확인").changedAt(changedAt).build());
	}

	private void review(Driver driver, int rating) {
		persist(Review.builder().shipment(ship(ShipmentStatus.COMPLETED)).customer(customer).driver(driver).rating(rating).build());
	}

	private Driver driver(String name) {
		return persist(Driver.builder().member(member(name, MemberRole.DRIVER)).licenseType("1종 보통").careerYears(3)
				.status(DriverStatus.ACTIVE).build());
	}

	private Member member(String name, MemberRole role) {
		return persist(Member.builder().email("hist-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com").password("x")
				.name(name).phone("010-0000-0000").role(role).enabled(true).build());
	}

	private Shipment ship(ShipmentStatus status) {
		return persist(Shipment.builder().customer(customer).vehicleType(vehicleType).cargoType(cargoType)
				.originAddress("서울특별시 중구 세종대로 110").destAddress("경기도 수원시 팔달구 효원로 1")
				.cargoName("[이력 통계 확인] 상자").pickupAt(FROM.plusHours(9))
				.senderName("보내는 사람").senderPhone("010-1111-2222")
				.receiverName("확인용").receiverPhone("010-0000-0000").fare(10_000).durationMin(60).status(status).build());
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}
}
