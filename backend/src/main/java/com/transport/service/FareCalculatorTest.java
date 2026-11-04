package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.transport.entity.CargoType;
import com.transport.entity.VehicleType;

class FareCalculatorTest {

	private final FareCalculator calculator = new FareCalculator();
	private VehicleType ton1;
	private CargoType general;
	private CargoType fragile;

	@BeforeEach
	void setUp() {
		// fare.md 1톤 값. 엔티티 생성 방식에 맞게 mock 대신 builder/생성자로 바꿔도 된다.
		ton1 = mock(VehicleType.class);
		when(ton1.getBaseFare()).thenReturn(50_000);
		when(ton1.getBaseDistanceKm()).thenReturn(10);
		when(ton1.getPerKmFare()).thenReturn(1_500);
		when(ton1.getNightRate()).thenReturn(20);
		when(ton1.getWeekendRate()).thenReturn(10);
		when(ton1.getHeavyRate()).thenReturn(10);
		when(ton1.getMaxLoadKg()).thenReturn(1_000);

		general = mock(CargoType.class);
		when(general.getExtraFee()).thenReturn(0);
		fragile = mock(CargoType.class);
		when(fragile.getExtraFee()).thenReturn(5_000);
	}

	@Test
	@DisplayName("예시 ① 기본: 1톤, 38.4km, 600kg, 일반, 목 09:00 → 93,500원")
	void example1_basic() {
		int fare = calculator.calculate(ton1, general, 38.4, 600,
				LocalDateTime.of(2026, 12, 3, 9, 0));
		assertThat(fare).isEqualTo(93_500);
	}

	@Test
	@DisplayName("예시 ② 할증: 1톤, 38.4km, 900kg, 일반, 토 22:30 → 130,900원")
	void example2_surcharge() {
		int fare = calculator.calculate(ton1, general, 38.4, 900,
				LocalDateTime.of(2026, 12, 5, 22, 30));
		assertThat(fare).isEqualTo(130_900);
	}

	@Test
	@DisplayName("예시 ③ 화물: 1톤, 38.4km, 600kg, 파손주의, 목 09:00 → 98,500원")
	void example3_cargo() {
		int fare = calculator.calculate(ton1, fragile, 38.4, 600,
				LocalDateTime.of(2026, 12, 3, 9, 0));
		assertThat(fare).isEqualTo(98_500);
	}
}
