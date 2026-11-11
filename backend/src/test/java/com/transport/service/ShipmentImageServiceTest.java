package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentImage;
import com.transport.domain.enums.ShipmentImageType;
import com.transport.dto.ShipmentImageResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentImageRepository;
import com.transport.repository.ShipmentRepository;
import com.transport.storage.FileStorage;
import com.transport.util.CustomFileUtil;

// ShipmentImageServiceImpl 단위 테스트 (5-1)
@ExtendWith(MockitoExtension.class)
class ShipmentImageServiceTest {

	@Mock
	ShipmentRepository shipmentRepository;

	@Mock
	ShipmentImageRepository shipmentImageRepository;

	@Mock
	DispatchService dispatchService;

	@Mock
	FileStorage fileStorage;

	@Mock
	CustomFileUtil customFileUtil;

	@InjectMocks
	ShipmentImageServiceImpl shipmentImageService;

	private static final Long SHIPMENT_ID = 10L;
	private static final Long DRIVER_MEMBER_ID = 3L; // 기사의 member.id (약속 표 5주차)

	private Shipment shipment;

	@BeforeEach
	void setUp() {
		shipment = Shipment.builder().id(SHIPMENT_ID).build();
	}

	private MockMultipartFile imageFile(String name) {
		return new MockMultipartFile("files", name, "image/jpeg", new byte[] { 1, 2, 3 });
	}

	@Test
	@DisplayName("업로드 성공 — 배정 기사가 PICKUP 사진 2장")
	void upload_success() {
		// given
		when(dispatchService.getActiveDriver(SHIPMENT_ID)).thenReturn(DRIVER_MEMBER_ID);
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(java.util.Optional.of(shipment));
		when(fileStorage.save(any(), eq("PICKUP"))).thenReturn("uuid_a.jpg", "uuid_b.jpg");
		when(shipmentImageRepository.save(any(ShipmentImage.class)))
				.thenAnswer(inv -> inv.getArgument(0));

		// when
		List<ShipmentImageResponseDTO> result = shipmentImageService.upload(
				SHIPMENT_ID, List.of(imageFile("a.jpg"), imageFile("b.jpg")),
				ShipmentImageType.PICKUP, DRIVER_MEMBER_ID);

		// then
		assertThat(result).hasSize(2);
		assertThat(result.get(0).getFileName()).isEqualTo("uuid_a.jpg");
		assertThat(result.get(0).getUrl()).isEqualTo("/api/files/uuid_a.jpg");
		assertThat(result.get(0).getType()).isEqualTo(ShipmentImageType.PICKUP);
		verify(fileStorage, org.mockito.Mockito.times(2)).save(any(), eq("PICKUP"));
		verify(shipmentImageRepository, org.mockito.Mockito.times(2)).save(any(ShipmentImage.class));
	}

	@Test
	@DisplayName("실패 — 배송이 없으면 SHIPMENT_NOT_FOUND")
	void upload_shipmentNotFound() {
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> shipmentImageService.upload(
				SHIPMENT_ID, List.of(imageFile("a.jpg")), ShipmentImageType.PICKUP, DRIVER_MEMBER_ID))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode())
						.isEqualTo(ErrorCode.SHIPMENT_NOT_FOUND));
	}

	@Test
	@DisplayName("실패 — 배정 기사가 아니면 NOT_ASSIGNED_DRIVER (파일 저장 안 함)")
	void upload_notAssignedDriver() {
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(java.util.Optional.of(shipment));
		when(dispatchService.getActiveDriver(SHIPMENT_ID)).thenReturn(999L); // 다른 기사

		assertThatThrownBy(() -> shipmentImageService.upload(
				SHIPMENT_ID, List.of(imageFile("a.jpg")), ShipmentImageType.PICKUP, DRIVER_MEMBER_ID))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode())
						.isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER));

		verify(fileStorage, never()).save(any(), anyString());
	}

	@Test
	@DisplayName("실패 — 배정이 아예 없으면(null) NOT_ASSIGNED_DRIVER")
	void upload_noAssignment() {
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(java.util.Optional.of(shipment));
		when(dispatchService.getActiveDriver(SHIPMENT_ID)).thenReturn(null);

		assertThatThrownBy(() -> shipmentImageService.upload(
				SHIPMENT_ID, List.of(imageFile("a.jpg")), ShipmentImageType.PICKUP, DRIVER_MEMBER_ID))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode())
						.isEqualTo(ErrorCode.NOT_ASSIGNED_DRIVER));
	}

	@Test
	@DisplayName("실패 — 파일이 비어있으면 INVALID_INPUT")
	void upload_emptyFiles() {
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(java.util.Optional.of(shipment));
		when(dispatchService.getActiveDriver(SHIPMENT_ID)).thenReturn(DRIVER_MEMBER_ID);

		assertThatThrownBy(() -> shipmentImageService.upload(
				SHIPMENT_ID, List.of(), ShipmentImageType.PICKUP, DRIVER_MEMBER_ID))
				.isInstanceOf(CustomException.class)
				.satisfies(e -> assertThat(((CustomException) e).getErrorCode())
						.isEqualTo(ErrorCode.INVALID_INPUT));
	}

	@Test
	@DisplayName("실패 — 파일 검증 통과 못 하면 예외 전파 (400 INVALID_INPUT, 저장 안 함)")
	void upload_invalidFile() {
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(java.util.Optional.of(shipment));
		when(dispatchService.getActiveDriver(SHIPMENT_ID)).thenReturn(DRIVER_MEMBER_ID);
		org.mockito.Mockito.doThrow(new CustomException(ErrorCode.INVALID_INPUT, "허용되지 않는 파일 형식입니다."))
				.when(customFileUtil).validateImage(any());

		assertThatThrownBy(() -> shipmentImageService.upload(
				SHIPMENT_ID, List.of(imageFile("evil.exe")), ShipmentImageType.PICKUP, DRIVER_MEMBER_ID))
				.isInstanceOf(CustomException.class)
				.hasMessage("허용되지 않는 파일 형식입니다.");

		verify(fileStorage, never()).save(any(), anyString());
	}
}