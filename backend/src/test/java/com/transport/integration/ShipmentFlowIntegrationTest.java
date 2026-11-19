package com.transport.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.FileSystemUtils;

import com.transport.client.TossPaymentClient;
import com.transport.domain.CargoType;
import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Vehicle;
import com.transport.domain.VehicleType;
import com.transport.domain.enums.DriverStatus;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.VehicleStatus;
import com.transport.service.DistanceService;
import com.transport.service.DistanceService.Route;
import com.transport.util.JWTUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// 신청 → 결제 → 기사 지원 · 고객 선택 → 사진 · 상태 변경 → 완료 → 리뷰 → 관리자 대시보드까지 실제 API로 한 번에 (W · #94)
// 도메인 4개가 약속한 Service 메서드로 이어지는지, 보안 필터(JWT) · 권한 · 상태 규칙까지 함께 확인한다
// 바깥 서비스만 가짜: 카카오 길찾기(DistanceService) · 토스 승인(TossPaymentClient). DB는 실제 MariaDB (Docker 필요), 테스트가 끝나면 롤백
// 사진 파일은 build/it-upload에 저장했다가 끝나면 지운다
@SpringBootTest(properties = "app.file.upload-dir=" + ShipmentFlowIntegrationTest.UPLOAD_DIR)
@AutoConfigureMockMvc
@Transactional
class ShipmentFlowIntegrationTest {

	static final String UPLOAD_DIR = "build/it-upload";

	// 1x1 PNG (상차 사진 · 인수증)
	private static final byte[] PNG = Base64.getDecoder()
			.decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@Autowired
	private JsonMapper jsonMapper;

	@PersistenceContext
	private EntityManager em;

	@MockitoBean
	private DistanceService distanceService;

	@MockitoBean
	private TossPaymentClient tossPaymentClient;

	private Member customer;
	private Member otherCustomer;
	private Member driverMember;
	private Member admin;
	private Driver driver;
	private Vehicle vehicle;
	private VehicleType vehicleType;
	private CargoType cargoType;

	@BeforeEach
	void setUp() {
		String suffix = UUID.randomUUID().toString().substring(0, 8);
		customer = member("흐름 고객", MemberRole.CUSTOMER);
		otherCustomer = member("흐름 다른 고객", MemberRole.CUSTOMER);
		driverMember = member("흐름 기사", MemberRole.DRIVER);
		admin = member("흐름 관리자", MemberRole.ADMIN);
		vehicleType = persist(VehicleType.builder().code("FLOW_" + suffix).name("흐름 1톤")
				.maxLoadKg(1000).baseFare(30_000).baseDistanceKm(10).perKmFare(1_000).active(true).build());
		cargoType = persist(CargoType.builder().name("흐름" + suffix).extraFee(0).active(true).build());
		driver = persist(Driver.builder().member(driverMember).licenseType("1종 보통").careerYears(3)
				.status(DriverStatus.ACTIVE).approvedAt(LocalDateTime.now()).build());
		vehicle = persist(Vehicle.builder().plateNo("IT" + suffix).vehicleType(vehicleType)
				.status(VehicleStatus.AVAILABLE).driver(driver).build());
		em.flush();

		// 서울시청 → 수원 12km · 30분 (카카오 대신)
		when(distanceService.findRoute(anyString(), anyString()))
				.thenReturn(new Route(12.0, 30, 37.5663, 126.9779, 37.2636, 127.0286));
	}

	@AfterAll
	static void removeUploadedPhotos() throws IOException {
		FileSystemUtils.deleteRecursively(Path.of(UPLOAD_DIR));
	}

	@Test
	void customerRequestToReviewFlow() throws Exception {
		JsonNode before = body(perform(get("/api/admin/dashboard"), admin).andExpect(status().isOk()));

		// 1. 고객: 예상 운임 → 배송 신청 (결제 대기). 신청 금액은 서버가 다시 계산한 운임과 같다
		Map<String, Object> request = shipmentRequest();
		int estimatedFare = body(perform(post("/api/fares/estimate"), customer, request).andExpect(status().isOk()))
				.get("totalFare").asInt();
		JsonNode created = body(perform(post("/api/shipments"), customer, request).andExpect(status().isCreated()));
		long shipmentId = created.get("shipmentId").asLong();
		String orderId = created.get("orderId").asString();
		int amount = created.get("amount").asInt();
		assertThat(created.get("status").asString()).isEqualTo("PENDING_PAYMENT");
		assertThat(amount).isEqualTo(estimatedFare).isPositive();

		// 2. 결제: 금액이 다르면 토스를 부르기 전에 막고, 맞으면 승인 → 접수(REQUESTED)
		perform(post("/api/payments/confirm"), customer, Map.of("orderId", orderId, "amount", amount + 1_000, "paymentKey", "flow-key"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PAYMENT_AMOUNT_MISMATCH"));
		verify(tossPaymentClient, never()).confirm(anyString(), anyString(), anyInt());
		perform(post("/api/payments/confirm"), customer, Map.of("orderId", orderId, "amount", amount, "paymentKey", "flow-key"))
				.andExpect(status().isOk());
		verify(tossPaymentClient).confirm("flow-key", orderId, amount);

		// 3. 기사: 운송 요청 목록에서 지원 가능 → 내 차량으로 지원
		JsonNode requests = body(perform(get("/api/driver/shipments"), driverMember).andExpect(status().isOk()));
		JsonNode listed = find(requests, "shipmentId", shipmentId);
		assertThat(listed).as("기사 운송 요청 목록에 신청한 배송").isNotNull();
		assertThat(listed.get("canApply").asBoolean()).isTrue();
		long applicationId = body(perform(post("/api/driver/shipments/" + shipmentId + "/applications"), driverMember,
				Map.of("vehicleId", vehicle.getId())).andExpect(status().isOk())).asLong();

		// 4. 고객: 지원한 기사 목록에서 선택 → 배차 확정(ASSIGNED), 기사의 내 배차 목록에 나온다
		JsonNode applicants = body(perform(get("/api/customer/shipments/" + shipmentId + "/applications"), customer)
				.andExpect(status().isOk()));
		assertThat(find(applicants, "applicationId", applicationId).get("driverId").asLong()).isEqualTo(driver.getId());
		perform(post("/api/customer/shipments/" + shipmentId + "/applications/" + applicationId + "/select"), customer)
				.andExpect(status().isOk());
		JsonNode myDispatches = body(perform(get("/api/driver/dispatches"), driverMember).andExpect(status().isOk()));
		assertThat(find(myDispatches, "shipmentId", shipmentId)).as("기사 내 배차 목록").isNotNull();

		// 5. 기사: 상차 사진 없이는 상차완료가 막히고, 사진을 올리면 상차 → 운송중
		changeStatus(shipmentId, "PICKED_UP").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PHOTO_REQUIRED"));
		uploadPhoto(shipmentId, "PICKUP");
		changeStatus(shipmentId, "PICKED_UP").andExpect(status().isNoContent());
		changeStatus(shipmentId, "IN_TRANSIT").andExpect(status().isNoContent());

		// 6. 위치: 운송중에만 받고 보여 준다
		perform(post("/api/shipments/" + shipmentId + "/locations"), driverMember, Map.of("lat", 37.40, "lng", 127.00))
				.andExpect(status().isOk());
		perform(get("/api/shipments/" + shipmentId + "/locations/latest"), customer)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lat").value(37.40));

		// 7. 기사: 도착 → 인수증 없이는 완료가 막히고, 올리면 배송완료. 도착 뒤에는 위치를 받지도 보여 주지도 않는다
		changeStatus(shipmentId, "ARRIVED").andExpect(status().isNoContent());
		perform(get("/api/shipments/" + shipmentId + "/locations/latest"), customer).andExpect(status().isNoContent());
		perform(post("/api/shipments/" + shipmentId + "/locations"), driverMember, Map.of("lat", 37.30, "lng", 127.01))
				.andExpect(status().is4xxClientError());
		changeStatus(shipmentId, "COMPLETED").andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("PHOTO_REQUIRED"));
		uploadPhoto(shipmentId, "RECEIPT");
		changeStatus(shipmentId, "COMPLETED").andExpect(status().isNoContent());

		// 8. 고객: 배송 상세(완료) · 이력. 다른 고객은 볼 수 없다
		JsonNode detail = body(perform(get("/api/shipments/" + shipmentId), customer).andExpect(status().isOk()));
		assertThat(detail.get("status").asString()).isEqualTo("COMPLETED");
		assertThat(detail.get("fare").asInt()).isEqualTo(amount);
		perform(get("/api/shipments/" + shipmentId), otherCustomer).andExpect(status().isForbidden());
		JsonNode history = body(perform(get("/api/shipments/" + shipmentId + "/history"), customer).andExpect(status().isOk()));
		List<String> statuses = new ArrayList<>();
		history.forEach(row -> statuses.add(row.get("toStatus").asString()));
		assertThat(statuses).containsExactly("REQUESTED", "ASSIGNED", "PICKED_UP", "IN_TRANSIT", "ARRIVED", "COMPLETED");

		// 9. 고객: 리뷰는 한 번만
		perform(post("/api/shipments/" + shipmentId + "/review"), customer, Map.of("rating", 5, "content", "흐름 확인"))
				.andExpect(status().isOk());
		perform(post("/api/shipments/" + shipmentId + "/review"), customer, Map.of("rating", 4))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("REVIEW_EXISTS"));

		// 10. 관리자 대시보드: 오늘 접수 · 오늘 완료 · 리뷰 수가 하나씩 늘고, 기사 순위에 이 기사가 나온다
		JsonNode after = body(perform(get("/api/admin/dashboard"), admin).andExpect(status().isOk()));
		assertThat(after.get("todayReceivedCount").asLong() - before.get("todayReceivedCount").asLong()).isEqualTo(1);
		assertThat(after.get("todayCompletedCount").asLong() - before.get("todayCompletedCount").asLong()).isEqualTo(1);
		assertThat(after.get("reviewCount").asLong() - before.get("reviewCount").asLong()).isEqualTo(1);
		if (before.get("driverRanks").size() < 5) {  // 이번 달 완료한 기사가 이미 5명이면 새 기사는 순위 밖일 수 있다
			JsonNode rank = find(after.get("driverRanks"), "driverId", driver.getId());
			assertThat(rank).as("기사 순위에 이 기사").isNotNull();
			assertThat(rank.get("completedCount").asLong()).isEqualTo(1);
			assertThat(rank.get("averageRating").asDouble()).isEqualTo(5.0);
		}
	}

	private ResultActions changeStatus(long shipmentId, String nextStatus) throws Exception {
		return perform(patch("/api/shipments/" + shipmentId + "/status"), driverMember, Map.of("nextStatus", nextStatus));
	}

	// 화면의 FormData처럼 files · type 두 조각으로 보낸다 (컨트롤러가 둘 다 @RequestPart)
	private void uploadPhoto(long shipmentId, String type) throws Exception {
		mockMvc.perform(multipart("/api/shipments/" + shipmentId + "/images")
						.file(new MockMultipartFile("files", "flow.png", MediaType.IMAGE_PNG_VALUE, PNG))
						.part(new MockPart("type", type.getBytes(StandardCharsets.UTF_8)))
						.header(HttpHeaders.AUTHORIZATION, token(driverMember)))
				.andExpect(status().isOk());
	}

	private ResultActions perform(MockHttpServletRequestBuilder request, Member who) throws Exception {
		return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, token(who)));
	}

	private ResultActions perform(MockHttpServletRequestBuilder request, Member who, Object json) throws Exception {
		return mockMvc.perform(request.header(HttpHeaders.AUTHORIZATION, token(who))
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonMapper.writeValueAsString(json)));
	}

	private JsonNode body(ResultActions result) throws Exception {
		return jsonMapper.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
	}

	// 목록에서 field 값이 value인 항목 (없으면 null)
	private JsonNode find(JsonNode list, String field, long value) {
		for (JsonNode row : list) {
			if (row.get(field).asLong() == value) {
				return row;
			}
		}
		return null;
	}

	// 다음 수요일 오전 10시 상차 (야간 · 주말 할증 없이)
	private Map<String, Object> shipmentRequest() {
		LocalDateTime pickupAt = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).atTime(10, 0);
		Map<String, Object> request = new LinkedHashMap<>();
		request.put("vehicleTypeId", vehicleType.getId());
		request.put("cargoTypeId", cargoType.getId());
		request.put("originAddress", "서울특별시 중구 세종대로 110");
		request.put("destAddress", "경기도 수원시 팔달구 효원로 1");
		request.put("cargoName", "[흐름 확인] 상자");
		request.put("cargoWeightKg", 300);
		request.put("pickupAt", pickupAt.toString());
		request.put("senderName", "보내는 사람");
		request.put("senderPhone", "010-1111-2222");
		request.put("receiverName", "받는 사람");
		request.put("receiverPhone", "010-3333-4444");
		return request;
	}

	private String token(Member member) {
		return "Bearer " + jwtUtil.createAccessToken(member);
	}

	private Member member(String name, MemberRole role) {
		return persist(Member.builder().email("flow-" + UUID.randomUUID().toString().substring(0, 8) + "@test.com")
				.password("x").name(name).phone("010-0000-0000").role(role).enabled(true).build());
	}

	private <T> T persist(T entity) {
		em.persist(entity);
		return entity;
	}
}
