package com.transport.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.transport.entity.CargoType;
import com.transport.entity.VehicleType;

/**
 * 운임 계산 (docs/fare.md). 운임은 이 클래스로만 계산한다.
 * 요금 값은 VehicleType · CargoType에서 읽는다.
 */
@Component
public class FareCalculator {

	public int calculate(VehicleType type, CargoType cargo,
			double distanceKm, int weightKg, LocalDateTime pickupAt) {
		int fare = type.getBaseFare() + distanceFare(type, distanceKm) + cargo.getExtraFee();

		int rate = 0; // 할증률 합산(%)
		int hour = pickupAt.getHour();
		if (hour >= 22 || hour < 6) {
			rate += type.getNightRate();
		}
		DayOfWeek day = pickupAt.getDayOfWeek();
		if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
			rate += type.getWeekendRate();
		}
		if (weightKg * 100 > type.getMaxLoadKg() * 80) { // 최대 적재 80% 초과
			rate += type.getHeavyRate();
		}

		int total = fare + fare * rate / 100;
		return total / 100 * 100; // 100원 미만 버림
	}

	/** 기본 거리 초과분 요금 (거리는 1km 단위로 올림) */
	public int distanceFare(VehicleType type, double distanceKm) {
		int overKm = Math.max(0, (int) Math.ceil(distanceKm) - type.getBaseDistanceKm());
		return overKm * type.getPerKmFare();
	}
}
