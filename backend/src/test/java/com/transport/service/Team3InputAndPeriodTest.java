package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.transport.controller.AdminCargoTypeController;
import com.transport.dto.AdminPaymentSearchDTO;
import com.transport.dto.AdminShipmentSearchDTO;
import com.transport.dto.CargoTypeRequestDTO;
import com.transport.dto.PageRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.PaymentRepository;
import com.transport.repository.ShipmentRepository;

import tools.jackson.databind.json.JsonMapper;

// 팀원 3 입력 처리 (11-25, #108 댓글) — 화물 종류 입력 기본값 · 사용 여부 빠짐, 검색 기간 거꾸로
class Team3InputAndPeriodTest {

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	// ===== 화물 종류 등록 · 수정 입력 =====

	@Test
	@DisplayName("화물 종류 입력: extraFee · active를 빼거나 null로 보내면 0원 · 사용함")
	void cargoTypeRequestDefaultsWhenMissingOrNull() throws Exception {
		CargoTypeRequestDTO missing = jsonMapper.readValue("{\"name\":\"냉장\"}", CargoTypeRequestDTO.class);
		CargoTypeRequestDTO nulls = jsonMapper.readValue("{\"name\":\"냉장\",\"extraFee\":null,\"active\":null}", CargoTypeRequestDTO.class);
		CargoTypeRequestDTO given = jsonMapper.readValue("{\"name\":\"냉장\",\"extraFee\":3000,\"active\":false}", CargoTypeRequestDTO.class);

		assertThat(missing.getExtraFee()).isZero();
		assertThat(missing.isActive()).isTrue();
		assertThat(nulls.getExtraFee()).isZero();
		assertThat(nulls.isActive()).isTrue();
		assertThat(given.getExtraFee()).isEqualTo(3000);
		assertThat(given.isActive()).isFalse();
	}

	@Test
	@DisplayName("화물 종류 사용 여부 바꾸기: active가 빠지거나 null이면 400, 바꾸지 않는다")
	void cargoTypeActiveRequiresValue() {
		CargoTypeService service = mock(CargoTypeService.class);
		AdminCargoTypeController controller = new AdminCargoTypeController(service);
		Map<String, Boolean> nullActive = new HashMap<>();
		nullActive.put("active", null);

		assertThatThrownBy(() -> controller.changeActive(3L, Map.of()))
				.isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		assertThatThrownBy(() -> controller.changeActive(3L, nullActive))
				.isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		verify(service, never()).changeCargoTypeActive(anyLong(), anyBoolean());

		controller.changeActive(3L, Map.of("active", false));
		verify(service).changeCargoTypeActive(3L, false);
	}

	// ===== 검색 기간 거꾸로 (시작일 > 종료일) =====

	private static final LocalDate LATER = LocalDate.of(2026, 11, 25);
	private static final LocalDate EARLIER = LocalDate.of(2026, 11, 1);

	@Test
	@DisplayName("관리자 배송 · 결제 검색: 시작일이 종료일보다 늦으면 400, 조회하지 않는다 (같은 날은 됨)")
	void adminSearchRejectsReversedPeriod() {
		ShipmentRepository shipmentRepository = mock(ShipmentRepository.class);
		PaymentRepository paymentRepository = mock(PaymentRepository.class);
		AdminShipmentService shipments = new AdminShipmentService(shipmentRepository);
		AdminPaymentService payments = new AdminPaymentService(paymentRepository);

		AdminShipmentSearchDTO shipmentSearch = new AdminShipmentSearchDTO();
		shipmentSearch.setFrom(LATER);
		shipmentSearch.setTo(EARLIER);
		AdminPaymentSearchDTO paymentSearch = new AdminPaymentSearchDTO();
		paymentSearch.setFrom(LATER);
		paymentSearch.setTo(EARLIER);

		assertThatThrownBy(() -> shipments.searchShipments(shipmentSearch))
				.isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		assertThatThrownBy(() -> payments.searchPayments(paymentSearch))
				.isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		verifyNoInteractions(shipmentRepository, paymentRepository);
	}

	@Test
	@DisplayName("고객 내 배송: 시작일이 종료일보다 늦으면 400")
	void myShipmentsRejectsReversedPeriod() {
		ShipmentRepository shipmentRepository = mock(ShipmentRepository.class);
		ShipmentServiceImpl service = new ShipmentServiceImpl(null, shipmentRepository, null, null, null, null, null, null, null);

		assertThatThrownBy(() -> service.getMyShipments(7L, new PageRequestDTO(), null, LATER, EARLIER))
				.isInstanceOf(CustomException.class).extracting("errorCode").isEqualTo(ErrorCode.INVALID_INPUT);
		verifyNoInteractions(shipmentRepository);
	}
}
