package com.transport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.Review;

// 리뷰 (8-1~8-2 — 팀원 4)
public interface ReviewRepository extends JpaRepository<Review, Long> {

	// 배송당 리뷰 1건 — 중복 작성 검사
	boolean existsByShipmentId(Long shipmentId);

	// 기사 리뷰 목록 (최신순) — N+1 방지를 위해 customer까지 한 번에 (fetch join)
	@Query("select r from Review r join fetch r.customer where r.driver.id = :driverId order by r.createdAt desc")
	List<Review> findByDriverIdWithCustomer(@Param("driverId") Long driverId);

	// 평균 평점 (리뷰가 없으면 null — 서비스에서 0.0으로 변환)
	@Query("select avg(r.rating) from Review r where r.driver.id = :driverId")
	Double averageRatingByDriverId(@Param("driverId") Long driverId);

	// 특정 배송의 리뷰 (이미 작성했는지 확인·보여주기용)
	Optional<Review> findByShipmentId(Long shipmentId);
}