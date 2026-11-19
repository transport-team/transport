package com.transport.service;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;

import lombok.extern.log4j.Log4j2;
import tools.jackson.databind.JsonNode;

@Log4j2
@Service
public class KakaoDistanceService implements DistanceService {

	private static final String GEOCODE_URL = "https://dapi.kakao.com/v2/local/search/address.json";
	private static final String DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";
	private static final double DETOUR_FACTOR = 1.3; // 직선거리 → 도로거리 보정 계수
	private static final double EARTH_RADIUS_KM = 6371.0;
	private static final double FALLBACK_SPEED_KMH = 40.0; // 길찾기 실패 시 평균 속도(km/h)
	
	private static final int MAX_CACHE_SIZE = 1000;
	private static final Duration ROUTE_CACHE_TTL = Duration.ofMinutes(30);
	private static final Duration GEOCODE_CACHE_TTL = Duration.ofHours(24);

	private record CacheEntry<T>(T value, Instant expiresAt) {
		boolean isExpired() {
			return Instant.now().isAfter(expiresAt);
		}
	}

	private final Map<String, CacheEntry<double[]>> geocodeCache = createLruCache(MAX_CACHE_SIZE);
	private final Map<String, CacheEntry<Route>> routeCache = createLruCache(MAX_CACHE_SIZE);

	private static final int MAX_CACHE_SIZE = 1000;
	private static final Duration ROUTE_CACHE_TTL = Duration.ofMinutes(30);
	private static final Duration GEOCODE_CACHE_TTL = Duration.ofHours(24);

	private record CacheEntry<T>(T value, Instant expiresAt) {
		boolean isExpired() {
			return Instant.now().isAfter(expiresAt);
		}
	}

	private final Map<String, CacheEntry<double[]>> geocodeCache = createLruCache(MAX_CACHE_SIZE);
	private final Map<String, CacheEntry<Route>> routeCache = createLruCache(MAX_CACHE_SIZE);

	private final RestTemplate restTemplate = new RestTemplate();
	private final String apiKey;

	public KakaoDistanceService(@Value("${app.kakao.rest-api-key:}") String apiKey) {
		this.apiKey = apiKey;
		if (apiKey.isBlank()) {
			log.warn("app.kakao.rest-api-key가 비어 있어요 — 주소 검색 · 길찾기 호출이 실패합니다 (application-local.yml 확인)");
		}
	}

	@Override
	public Route findRoute(String originAddress, String destAddress) {
		String normOrigin = normalizeAddress(originAddress);
		String normDest = normalizeAddress(destAddress);
		String routeKey = normOrigin + "->" + normDest;

		CacheEntry<Route> cached = routeCache.get(routeKey);
		if (cached != null && !cached.isExpired()) {
			log.debug("구간 거리 캐시 히트: {}", routeKey);
			return cached.value();
		}

		double[] origin = geocode(normOrigin); // {경도, 위도}
		double[] dest = geocode(normDest);

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

			if (durationMin <= 0) {
				throw new IllegalStateException("길찾기 예상 운송 시간이 올바르지 않습니다.");
			}

			Route result = new Route(distanceKm, durationMin, origin[1], origin[0], dest[1], dest[0]);
			routeCache.put(routeKey, new CacheEntry<>(result, Instant.now().plus(ROUTE_CACHE_TTL)));
			return result;

		} catch (Exception e) {
		    log.warn("길찾기 실패, 직선거리 보정값 사용: {}", e.getMessage());

		    // 직선거리 대체 계산 결과는 일시적 API 오류일 수 있으므로 캐싱하지 않는다
		    double distanceKm = haversineKm(origin[1], origin[0], dest[1], dest[0]) * DETOUR_FACTOR;
		    int fallbackDurationMin = Math.max(1, (int) Math.ceil((distanceKm / FALLBACK_SPEED_KMH) * 60.0));

		    return new Route(distanceKm, fallbackDurationMin, origin[1], origin[0], dest[1], dest[0]);
		}
	}

	private double[] geocode(String normalizedAddress) {
		CacheEntry<double[]> cached = geocodeCache.get(normalizedAddress);
		if (cached != null && !cached.isExpired()) {
			return cached.value();
		}

		if (apiKey.isBlank()) {
			throw new CustomException(ErrorCode.ADDRESS_NOT_FOUND, "주소 검색 서비스가 설정되지 않았습니다.");
		}

		URI uri = UriComponentsBuilder.fromUriString(GEOCODE_URL)
				.queryParam("query", normalizedAddress)
				.build().encode().toUri();
		try {
			JsonNode documents = get(uri).path("documents");
			if (documents.isEmpty()) {
				throw new CustomException(ErrorCode.ADDRESS_NOT_FOUND, "주소를 찾을 수 없습니다: " + normalizedAddress);
			}
			JsonNode first = documents.get(0); // x = 경도, y = 위도
			double[] coords = new double[] { first.path("x").asDouble(), first.path("y").asDouble() };
			geocodeCache.put(normalizedAddress, new CacheEntry<>(coords, Instant.now().plus(GEOCODE_CACHE_TTL)));
			return coords;
		} catch (CustomException exception) {
			throw exception;
		} catch (RestClientException | IllegalStateException exception) {
			log.warn("카카오 주소 검색 실패: address={}, message={}", normalizedAddress, exception.getMessage());
			throw new CustomException(ErrorCode.ADDRESS_NOT_FOUND, "주소를 확인할 수 없습니다: " + normalizedAddress);
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

	private static String normalizeAddress(String address) {
		return (address == null) ? "" : address.trim().replaceAll("\\s+", " ");
	}

	private static <K, V> Map<K, V> createLruCache(int maxSize) {
		return Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > maxSize;
			}
		});
	}

	private static double haversineKm(double lat1, double lng1, double lat2, double lng2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLng = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
		return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
	}
}