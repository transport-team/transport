package com.transport.service;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 리뷰 서비스 구현 (8-1~8-2 — 팀원 4)
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class ReviewServiceImpl implements ReviewService {

	private final ReviewRepository reviewRepository;
	// 다른 도메인은 약속 메서드로만 (배송 = 팀원 3, 배차 · 기사 = 팀원 2 — 저장소 직접 사용 정리, #94)
	private final ShipmentService shipmentService;
	private final DispatchService dispatchService;
	private final DriverService driverService;

	@Override
	public ReviewResponseDTO createReview(Long shipmentId, Long memberId, int rating, String content) {

		// 1. 배송 조회 (없으면 SHIPMENT_NOT_FOUND)
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 2. 본인 배송인지
		if (!shipment.getCustomer().getId().equals(memberId)) {
			throw new CustomException(ErrorCode.FORBIDDEN, "본인의 배송에만 리뷰를 작성할 수 있습니다.");
		}

		// 3. 배송완료 건만
		if (shipment.getStatus() != ShipmentStatus.COMPLETED) {
			throw new CustomException(ErrorCode.REVIEW_NOT_ALLOWED);
		}

		// 4. 배송당 1회 (중복 시 REVIEW_EXISTS)
		if (reviewRepository.existsByShipmentId(shipmentId)) {
			throw new CustomException(ErrorCode.REVIEW_EXISTS);
		}

		// 5. driver_id는 배정 기사로 설정 (요청에서 받지 않는다 — 위변조 방지)
		//    배송이 완료돼도 배차는 남아 있어 배정 기사를 찾을 수 있다 (11-19 결정 34)
		Long driverMemberId = dispatchService.getActiveDriver(shipmentId);  // 배정 기사의 회원 ID
		if (driverMemberId == null) {
			throw new CustomException(ErrorCode.NOT_FOUND, "배정 기사를 찾을 수 없습니다.");
		}
		Driver driver = driverService.findByMemberId(driverMemberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND, "배정 기사를 찾을 수 없습니다."));

		// 4번 검사와 저장 사이에 같은 요청이 동시에 들어오면(두 탭 · 다시 시도) DB 유니크 키(shipment_id)가 막는다
		// → 그 오류를 500이 아니라 4번과 같은 REVIEW_EXISTS(409)로 돌려준다
		Review saved;
		try {
			saved = reviewRepository.save(Review.builder()
					.shipment(shipment)
					.customer(shipment.getCustomer())
					.driver(driver)
					.rating(rating)
					.content(content)
					.build());
		} catch (DataIntegrityViolationException e) {
			throw new CustomException(ErrorCode.REVIEW_EXISTS);
		}

		log.info("리뷰 저장: shipmentId={}, driverId={}, rating={}", shipmentId, driver.getId(), rating);
		return ReviewResponseDTO.from(saved);
	}

	@Override
	@Transactional(readOnly = true)
	public ReviewResponseDTO getReviewByShipment(Long shipmentId) {
		return reviewRepository.findByShipmentId(shipmentId)
				.map(ReviewResponseDTO::from)
				.orElse(null);
	}

	@Override
	@Transactional(readOnly = true)
	public List<ReviewResponseDTO> getDriverReviews(Long driverId) {
		return reviewRepository.findByDriverIdWithCustomer(driverId).stream()
				.map(ReviewResponseDTO::from)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public double getAverageRating(Long driverId) {
		Double avg = reviewRepository.averageRatingByDriverId(driverId);
		// 리뷰가 없으면 0.0 — 약속 문서에 명시 (팀원 2가 정렬에 바로 사용)
		return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
	}

	@Override
	@Transactional(readOnly = true)
	public RatingStatDTO getRatingStat() {
		ReviewRepository.RatingSum sum = reviewRepository.sumAll();
		Double avg = sum.getAverageRating();
		return RatingStatDTO.builder()
				.reviewCount(sum.getReviewCount())
				.averageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0)
				.build();
	}
}