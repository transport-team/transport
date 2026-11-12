package com.transport.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.mock.web.MockPart;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.transport.config.SecurityConfig;
import com.transport.domain.Member;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentImageType;
import com.transport.service.ShipmentImageService;
import com.transport.util.JWTUtil;

// 배송 사진 업로드 API (5-1 후속): 사진 종류(type)가 틀리면 500이 아니라 400 (보안 필터를 켜고 확인)
@WebMvcTest(controllers = ShipmentImageController.class, properties = "app.jwt.secret=" + ShipmentImageControllerTest.SECRET)
@Import({SecurityConfig.class, JWTUtil.class})
class ShipmentImageControllerTest {

	static final String SECRET = "test-secret-key-for-jwt-signing-32bytes-or-more";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JWTUtil jwtUtil;

	@MockitoBean
	private ShipmentImageService shipmentImageService;

	private final MockMultipartFile photo =
			new MockMultipartFile("files", "pickup.jpg", "image/jpeg", new byte[] {1, 2, 3});

	@Test
	void unknownTypeIsBadRequest() throws Exception {
		mockMvc.perform(multipart("/api/shipments/1/images")
						.file(photo)
						.part(new MockPart("type", "SELFIE".getBytes(StandardCharsets.UTF_8)))
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.DRIVER)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"))
				.andExpect(jsonPath("$.message").value("사진 종류는 PICKUP(상차) · RECEIPT(인수증) · ETC(기타) 중 하나여야 합니다."));
		verifyNoInteractions(shipmentImageService);
	}

	@Test
	void lowerCaseTypeIsAccepted() throws Exception {
		when(shipmentImageService.upload(anyLong(), anyList(), any(), anyLong())).thenReturn(List.of());

		mockMvc.perform(multipart("/api/shipments/1/images")
						.file(photo)
						.part(new MockPart("type", "pickup".getBytes(StandardCharsets.UTF_8)))
						.header(HttpHeaders.AUTHORIZATION, token(MemberRole.DRIVER)))
				.andExpect(status().isOk());
		verify(shipmentImageService).upload(eq(1L), anyList(), eq(ShipmentImageType.PICKUP), eq(1L));
	}

	private String token(MemberRole role) {
		return "Bearer " + jwtUtil.createAccessToken(Member.builder().id(1L).email("driver@test.com").role(role).build());
	}
}
