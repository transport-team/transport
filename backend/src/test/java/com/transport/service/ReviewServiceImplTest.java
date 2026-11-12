package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.transport.domain.Dispatch;
import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Review;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.ReviewResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.ReviewRepository;
import com.transport.repository.ShipmentRepository;

// 리뷰 작성 (8-1 후속): 본인 · 배송완료 · 1회, 동시에 두 번 저장돼도 500이 아니라 REVIEW_EXISTS
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

	private static final Long SHIPMENT_ID = 1L;
	private static final Long CUSTOMER_ID = 100L;

	@Mock
	ReviewRepository reviewRepository;

	@Mock
	ShipmentRepository shipmentRepository;

	@Mock
	DriverRepository driverRepository;

	@Mock
	DispatchRepository dispatchRepository;

	@InjectMocks
	ReviewServiceImpl reviewService;

	private final Member customer = Member.builder().id(CUSTOMER_ID).name("테스트고객").build();
	private final Driver driver = Driver.builder().id(1L).build();

	private Shipment givenShipment(ShipmentStatus status) {
		Shipment shipment = Shipment.builder().id(SHIPMENT_ID).status(status).customer(customer).build();
		when(shipmentRepository.findById(SHIPMENT_ID)).thenReturn(Optional.of(shipment));
		lenient().when(reviewRepository.existsByShipmentId(SHIPMENT_ID)).thenReturn(false);
		lenient().when(dispatchRepository.findByShipmentIdAndActiveTrue(SHIPMENT_ID))
				.thenReturn(Optional.of(Dispatch.builder().shipment(shipment).driver(driver).active(true).build()));
		lenient().when(driverRepository.findById(1L)).thenReturn(Optional.of(driver));
		return shipment;
	}

	@Test
	@DisplayName("배송완료 · 본인 배송이면 저장하고, 기사는 배정 배차에서 정한다")
	void createReview() {
		Shipment shipment = givenShipment(ShipmentStatus.COMPLETED);
		when(reviewRepository.save(any(Review.class))).thenReturn(Review.builder()
				.id(7L).shipment(shipment).customer(customer).driver(driver).rating(5).content("친절해요").build());

		ReviewResponseDTO result = reviewService.createReview(SHIPMENT_ID, CUSTOMER_ID, 5, "친절해요");

		assertThat(result.getId()).isEqualTo(7L);
		assertThat(result.getDriverId()).isEqualTo(1L);
		assertThat(result.getRating()).isEqualTo(5);
	}

	@Test
	@DisplayName("이미 리뷰가 있으면 409 REVIEW_EXISTS (저장하지 않는다)")
	void alreadyReviewed() {
		givenShipment(ShipmentStatus.COMPLETED);
		when(reviewRepository.existsByShipmentId(SHIPMENT_ID)).thenReturn(true);

		CustomException ex = assertThrows(CustomException.class,
				() -> reviewService.createReview(SHIPMENT_ID, CUSTOMER_ID, 5, null));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_EXISTS);
		verify(reviewRepository, never()).save(any(Review.class));
	}

	@Test
	@DisplayName("동시에 두 번 들어와 DB 유니크 키에 걸려도 500이 아니라 REVIEW_EXISTS")
	void concurrentDuplicateIsReviewExists() {
		givenShipment(ShipmentStatus.COMPLETED);
		when(reviewRepository.save(any(Review.class)))
				.thenThrow(new DataIntegrityViolationException("Duplicate entry for key shipment_id"));

		CustomException ex = assertThrows(CustomException.class,
				() -> reviewService.createReview(SHIPMENT_ID, CUSTOMER_ID, 4, null));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_EXISTS);
	}

	@Test
	@DisplayName("배송완료 전이면 400 REVIEW_NOT_ALLOWED")
	void notCompleted() {
		givenShipment(ShipmentStatus.ARRIVED);

		CustomException ex = assertThrows(CustomException.class,
				() -> reviewService.createReview(SHIPMENT_ID, CUSTOMER_ID, 5, null));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED);
	}

	@Test
	@DisplayName("남의 배송이면 403")
	void otherCustomer() {
		givenShipment(ShipmentStatus.COMPLETED);

		CustomException ex = assertThrows(CustomException.class,
				() -> reviewService.createReview(SHIPMENT_ID, 999L, 5, null));

		assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN);
	}
}
