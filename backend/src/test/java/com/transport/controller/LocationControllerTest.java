package com.transport.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.transport.config.SecurityConfig;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.service.LocationService;
import com.transport.util.JWTUtil;

// 위치 전송 API (7-1 후속): 좌표 범위 밖 값은 저장하기 전에 400 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = LocationController.class, properties = "app.jwt.secret=" + LocationControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class LocationControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private LocationService locationService;

	@Test
	void latitudeOutOfRangeIsBadRequest() throws Exception {
		send("{\"lat\": 999, \"lng\": 127.0}", "위도(lat)는 -90 ~ 90 사이여야 합니다.");
		send("{\"lat\": -90.5, \"lng\": 127.0}", "위도(lat)는 -90 ~ 90 사이여야 합니다.");
		verifyNoInteractions(locationService);
	}

	@Test
	void longitudeOutOfRangeIsBadRequest() throws Exception {
		send("{\"lat\": 37.5, \"lng\": 181}", "경도(lng)는 -180 ~ 180 사이여야 합니다.");
		verifyNoInteractions(locationService);
	}

	private void send(String body, String message) throws Exception {
		mockMvc.perform(post("/api/shipments/1/locations")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtil.createAccessToken(
								Member.builder().id(3L).email("driver@test.com").role(MemberRole.DRIVER).build()))
						.contentType(MediaType.APPLICATION_JSON)
						.content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value(message));
	}
}
