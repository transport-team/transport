package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.transport.domain.Driver;
import com.transport.domain.Member;
import com.transport.domain.Review;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.RatingStatDTO;
import com.transport.dto.ReviewResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ReviewRepository;

// 리뷰 작성 (8-1 후속): 본인 · 배송완료 · 1회, 동시에 두 번 저장돼도 500이 아니라 REVIEW_EXISTS
// 배송 · 배차 · 기사는 약속 메서드(ShipmentService · DispatchService · DriverService)를 가짜로 둔다 (#94 저장소 직접 사용 정리)
@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

	private static final Long SHIPMENT_ID = 1L;
	private static final Long CUSTOMER_ID = 100L;
	private static final Long DRIVER_MEMBER_ID = 30L;  // 배정 기사의 회원 ID (getActiveDriver가 주는 값)

	@Mock
	ReviewRepository reviewRepository;

	@Mock
	ShipmentService shipmentService;

	@Mock
	DriverService driverService;

	@Mock
	DispatchService dispatchService;

	@InjectMocks
	ReviewServiceImpl reviewService;

	private final Member customer = Member.builder().id(CUSTOMER_ID).name("테스트고객").build();
	private final Driver driver = Driver.builder().id(1L).build();

	private Shipment givenShipment(ShipmentStatus status) {
		Shipment shipment = Shipment.builder().id(SHIPMENT_ID).status(status).customer(customer).build();
		when(shipmentService.getShipment(SHIPMENT_ID)).thenReturn(shipment);
		lenient().when(reviewRepository.existsByShipmentId(SHIPMENT_ID)).thenReturn(false);
		lenient().when(dispatchService.getActiveDriver(SHIPMENT_ID)).thenReturn(DRIVER_MEMBER_ID);
		lenient().when(driverService.findByMemberId(DRIVER_MEMBER_ID)).thenReturn(Optional.of(driver));
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
		assertThat(result.getCustomerName()).isEqualTo("테***객");  // 작성 응답도 가린다 (11-19 결정 4)
	}

	// ===== 고객 이름 가리기 (11-19 결정 4): 리뷰는 로그인 사용자면 누구나 볼 수 있어서 이름은 가려서 준다 =====

	@Test
	@DisplayName("기사 리뷰 목록은 작성한 고객 이름을 가려서 준다 (김민수 → 김*수)")
	void driverReviewsMaskCustomerName() {
		when(reviewRepository.findByDriverIdWithCustomer(1L)).thenReturn(List.of(reviewBy("김민수"), reviewBy("이민")));

		List<ReviewResponseDTO> reviews = reviewService.getDriverReviews(1L);

		assertThat(reviews).extracting(ReviewResponseDTO::getCustomerName).containsExactly("김*수", "이*");
	}

	@Test
	@DisplayName("내가 작성한 리뷰는 로그인 고객 ID로 조회한다")
	void myWrittenReviews() {
		when(reviewRepository.findByCustomerIdWithDetails(CUSTOMER_ID))
				.thenReturn(List.of(reviewBy("김민수")));

		List<ReviewResponseDTO> reviews = reviewService.getMyWrittenReviews(CUSTOMER_ID);

		assertThat(reviews).hasSize(1);
		assertThat(reviews.get(0).getShipmentId()).isEqualTo(SHIPMENT_ID);
		assertThat(reviews.get(0).getRating()).isEqualTo(4);
		verify(reviewRepository).findByCustomerIdWithDetails(CUSTOMER_ID);
	}

	@Test
	@DisplayName("배송 리뷰 조회도 고객 이름을 가려서 준다")
	void shipmentReviewMasksCustomerName() {
		when(reviewRepository.findByShipmentId(SHIPMENT_ID)).thenReturn(Optional.of(reviewBy("남궁현석")));

		ReviewResponseDTO review = reviewService.getReviewByShipment(SHIPMENT_ID);

		assertThat(review.getCustomerName()).isEqualTo("남**석");
	}

	private Review reviewBy(String customerName) {
		Member writer = Member.builder().id(200L).name(customerName).build();
		Shipment shipment = Shipment.builder().id(SHIPMENT_ID).status(ShipmentStatus.COMPLETED).customer(writer).build();
		return Review.builder().id(9L).shipment(shipment).customer(writer).driver(driver).rating(4).content("좋아요").build();
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

	// ===== 관리자 대시보드 집계 (쿼리 자체는 StatusHistoryReviewStatsTest가 실제 DB로 확인) =====

	@Test
	@DisplayName("전체 평균 평점은 소수 첫째 자리로 반올림하고 리뷰 수와 함께 준다")
	void ratingStat() {
		when(reviewRepository.sumAll()).thenReturn(ratingSum(3L, 14.0 / 3));  // 4.666…

		RatingStatDTO stat = reviewService.getRatingStat();

		assertThat(stat.getReviewCount()).isEqualTo(3);
		assertThat(stat.getAverageRating()).isEqualTo(4.7);
	}

	@Test
	@DisplayName("리뷰가 없으면 0개 · 0.0 (평균이 null이어도 오류 없이)")
	void ratingStatWithoutReviews() {
		when(reviewRepository.sumAll()).thenReturn(ratingSum(0L, null));

		RatingStatDTO stat = reviewService.getRatingStat();

		assertThat(stat.getReviewCount()).isZero();
		assertThat(stat.getAverageRating()).isZero();
	}

	private ReviewRepository.RatingSum ratingSum(Long count, Double average) {
		return new ReviewRepository.RatingSum() {
			@Override
			public Long getReviewCount() {
				return count;
			}

			@Override
			public Double getAverageRating() {
				return average;
			}
		};
	}
}
