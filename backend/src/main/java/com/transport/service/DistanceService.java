package com.transport.service;

/**
 * 주소 → 좌표 · 도로 거리 · 소요시간. 거리는 서버가 계산한다 (화면이 보낸 값은 믿지 않는다).
 */
public interface DistanceService {

	Route findRoute(String originAddress, String destAddress);

	/** durationMin은 길찾기 실패로 직선거리 보정값을 쓴 경우 null */
	record Route(double distanceKm, Integer durationMin,
			double originLat, double originLng, double destLat, double destLng) {
	}
}
