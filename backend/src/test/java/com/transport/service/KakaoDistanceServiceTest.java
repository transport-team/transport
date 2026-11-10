package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import com.transport.service.DistanceService.Route;

// 카카오 거리 계산: 실제 카카오 대신 가짜 응답으로 응답 읽기 · 길찾기 실패 시 직선거리 보정을 확인한다
class KakaoDistanceServiceTest {

	private static final String GEOCODE_URL = "https://dapi.kakao.com/v2/local/search/address.json";
	private static final String DIRECTIONS_URL = "https://apis-navi.kakaomobility.com/v1/directions";

	private KakaoDistanceService distanceService;
	private MockRestServiceServer server;

	@BeforeEach
	void setUp() {
		distanceService = new KakaoDistanceService("test-key");
		RestTemplate restTemplate = (RestTemplate) ReflectionTestUtils.getField(distanceService, "restTemplate");
		server = MockRestServiceServer.bindTo(restTemplate).build();
	}

	@Test
	void roadDistanceFromDirections() {
		givenAddresses();
		server.expect(requestTo(startsWith(DIRECTIONS_URL)))
				.andRespond(withSuccess("{\"routes\": [{\"result_code\": 0, \"summary\": {\"distance\": 38400, \"duration\": 3540}}]}",
						MediaType.APPLICATION_JSON));

		Route route = distanceService.findRoute("서울 중구 세종대로 110", "수원 팔달구 효원로 241");

		assertThat(route.distanceKm()).isEqualTo(38.4);
		assertThat(route.durationMin()).isEqualTo(59);
		assertThat(route.originLat()).isEqualTo(37.5663);
		assertThat(route.destLng()).isEqualTo(127.0286);
		server.verify();
	}

	@Test
	void straightLineWhenDirectionsFail() {
		givenAddresses();
		server.expect(requestTo(startsWith(DIRECTIONS_URL)))
				.andRespond(withSuccess("{\"routes\": [{\"result_code\": 104, \"result_msg\": \"too close\"}]}", MediaType.APPLICATION_JSON));

		Route route = distanceService.findRoute("서울 중구 세종대로 110", "수원 팔달구 효원로 241");

		// 길찾기 실패 → 보정 거리와 평균 속도 40km/h로 예상 시간 계산
		assertThat(route.durationMin()).isEqualTo(67);
		assertThat(route.distanceKm()).isCloseTo(44.1, within(0.5));
		server.verify();
	}

    // 길찾기 응답의 예상 시간이 0분이면 대체 시간 계산
    @Test
    void fallbackWhenDurationIsZero() {
            givenAddresses();

            // 카카오 길찾기는 성공했지만 예상 운송 시간이 0초인 상황
            server.expect(requestTo(startsWith(DIRECTIONS_URL)))
                            .andRespond(withSuccess(
                                            "{\"routes\": [{\"result_code\": 0, \"summary\": {\"distance\": 38400, \"duration\": 0}}]}",
                                            MediaType.APPLICATION_JSON));

            Route route = distanceService.findRoute(
                            "서울 중구 세종대로 110",
                            "수원 팔달구 효원로 241");

            // 0분 대신 직선거리 보정값으로 계산한 예상 시간 반환
            assertThat(route.durationMin()).isEqualTo(67);
            assertThat(route.distanceKm()).isCloseTo(44.1, within(0.5));

            server.verify();
    }

    // 출발지와 도착지가 같아도 예상 시간은 최소 1분 보장
    @Test
    void fallbackDurationAtLeastOneMinute() {
            String addressResponse =
                    "{\"documents\": [{\"x\": \"126.9779\", \"y\": \"37.5663\"}]}";

            // 출발지와 도착지가 동일한 좌표
            server.expect(requestTo(startsWith(GEOCODE_URL)))
                            .andRespond(withSuccess(addressResponse, MediaType.APPLICATION_JSON));

            server.expect(requestTo(startsWith(GEOCODE_URL)))
                            .andRespond(withSuccess(addressResponse, MediaType.APPLICATION_JSON));

            // 길찾기 실패 상황 재현
            server.expect(requestTo(startsWith(DIRECTIONS_URL)))
                            .andRespond(withSuccess(
                                            "{\"routes\": [{\"result_code\": 104, \"result_msg\": \"too close\"}]}",
                                            MediaType.APPLICATION_JSON));

            Route route = distanceService.findRoute("서울시청", "서울시청");

            // 계산된 거리가 0km여도 예상 운송 시간은 최소 1분
            assertThat(route.distanceKm()).isZero();
            assertThat(route.durationMin()).isEqualTo(1);

            server.verify();
    }

	// 출발지 · 도착지 주소 → 좌표 (x = 경도, y = 위도), 키는 KakaoAK 헤더로 보낸다
	private void givenAddresses() {
		server.expect(requestTo(startsWith(GEOCODE_URL)))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "KakaoAK test-key"))
				.andRespond(withSuccess("{\"documents\": [{\"x\": \"126.9779\", \"y\": \"37.5663\"}]}", MediaType.APPLICATION_JSON));
		server.expect(requestTo(startsWith(GEOCODE_URL)))
				.andRespond(withSuccess("{\"documents\": [{\"x\": \"127.0286\", \"y\": \"37.2636\"}]}", MediaType.APPLICATION_JSON));
	}
}
