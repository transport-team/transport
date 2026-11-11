package com.transport.service;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode; // Boot 4의 RestTemplate은 Jackson 3만 읽는다 (Jackson 2 JsonNode는 변환 실패)

/**
 * docs/fare.md "거리 구하기": 카카오 로컬(주소 → 좌표) + 카카오모빌리티 길찾기(도로 거리). 길찾기 실패 시 하버사인
 * 직선거리 × 1.3. 외부 API는 서버에서만 호출한다. 길찾기 요청 · 응답 형식은 카카오모빌리티 디벨로퍼스 문서로 한 번 더 확인할
 * 것.
 */
@Log4j2
@Service
public class KakaoDistanceService implements DistanceService {

	private static final String GEOCODE_URL = "https://dapi.kakao.com/v2/local/search/address.json";
	private static final String DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";
	private static final double DETOUR_FACTOR = 1.3; // 직선거리 → 도로거리 보정 계수
	// 길찾기 실패 시 예상 운송 시간 계산용 평균 속도 (임시 기준)
	private static final double EARTH_RADIUS_KM = 6371.0;

	private final RestTemplate restTemplate = new RestTemplate();
	private final String apiKey;

	// 키 이름은 팀 설정(application-local.yml.example)의 app.kakao.rest-api-key. 비어 있어도 서버는
	// 뜨게 기본값을 둔다
	public KakaoDistanceService(@Value("${app.kakao.rest-api-key:}") String apiKey) {
		this.apiKey = apiKey;
		if (apiKey.isBlank()) {
			log.warn("app.kakao.rest-api-key가 비어 있어요 — 주소 검색 · 길찾기 호출이 실패합니다 (application-local.yml 확인)");
		}
	}

	@Override
	public Route findRoute(String originAddress, String destAddress) {
		double[] origin = geocode(originAddress); // {경도, 위도}
		double[] dest = geocode(destAddress);

		try {
			URI uri = UriComponentsBuilder.fromUriString(DIRECTIONS_URL)
					.queryParam("origin", origin[0] + "," + origin[1])
					.queryParam("destination", dest[0] + "," + dest[1]).build().encode().toUri();
			JsonNode route = get(uri).path("routes").path(0);
			if (route.path("result_code").asInt(-1) != 0) {
				throw new IllegalStateException("길찾기 결과 없음: " + route.path("result_msg").asString());
			}
			JsonNode summary = route.path("summary");
			double distanceKm = summary.path("distance").asDouble() / 1000.0;
			int durationMin = (int) Math.ceil(summary.path("duration").asDouble() / 60.0);

			if (distanceKm <= 0) {
				throw new IllegalStateException("길찾기 거리가 올바르지 않습니다.");
			}

			// 길찾기 응답에 예상 시간이 없거나 0분인 경우 대체 계산으로 전환
			if (durationMin <= 0) {
				throw new IllegalStateException("길찾기 예상 운송 시간이 올바르지 않습니다.");
			}

			return new Route(distanceKm, durationMin, origin[1], origin[0], dest[1], dest[0]);

		} catch (Exception e) {
			log.warn("길찾기 실패, 직선거리 보정값 사용: {}", e.getMessage());

			// 기존 방식대로 직선거리를 도로거리로 보정
			double distanceKm = haversineKm(origin[1], origin[0], dest[1], dest[0]) * DETOUR_FACTOR;

<<<<<<< HEAD
			return new Route(distanceKm, null, origin[1], origin[0], dest[1], dest[0]);
=======
			// 평균 속도 40km/h를 가정하여 예상 운송 시간 계산 (최소 1분 보장)
			int durationMin = Math.max(1, (int) Math.ceil(distanceKm / FALLBACK_SPEED_KMH * 60));

			return new Route(distanceKm, durationMin, origin[1], origin[0], dest[1], dest[0]);
>>>>>>> develop
		}
	}

	private double[] geocode(String address) {
		if (apiKey.isBlank()) {
			throw new CustomException(ErrorCode.ADDRESS_NOT_FOUND, "주소 검색 서비스가 설정되지 않았습니다.");
		}

		URI uri = UriComponentsBuilder.fromUriString(GEOCODE_URL)
				.queryParam("query", address)
				.build().encode().toUri();
		try {
			JsonNode documents = get(uri).path("documents");
			if (documents.isEmpty()) {
				throw new CustomException(ErrorCode.ADDRESS_NOT_FOUND, "주소를 찾을 수 없습니다: " + address);
			}
			JsonNode first = documents.get(0); // x = 경도, y = 위도
			return new double[] { first.path("x").asDouble(), first.path("y").asDouble() };
		} catch (CustomException exception) {
			throw exception;
		} catch (RestClientException | IllegalStateException exception) {
			log.warn("카카오 주소 검색 실패: address={}, message={}", address, exception.getMessage());
			throw new CustomException(ErrorCode.ADDRESS_NOT_FOUND, "주소를 확인할 수 없습니다: " + address);
		}
	}

	private JsonNode get(URI uri) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey);
		JsonNode body = restTemplate.exchange(uri, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
		if (body == null || body.isMissingNode()) {
			throw new IllegalStateException("카카오 API 응답이 비어 있습니다.");
		}
		return body;
	}

	private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}
}