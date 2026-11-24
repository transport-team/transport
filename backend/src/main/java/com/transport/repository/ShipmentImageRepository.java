package com.transport.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.transport.domain.ShipmentImage;
import com.transport.domain.enums.ShipmentImageType;

// 배송 사진 리포지토리
public interface ShipmentImageRepository extends JpaRepository<ShipmentImage, Long> {

	/**
	 * 배송 ID로 사진 목록 조회 (타입별 정렬)
	 */
	List<ShipmentImage> findByShipmentIdOrderByTypeAscCreatedAtAsc(Long shipmentId);

	/**
	 * 배송 ID + 타입으로 사진 조회
	 */
	List<ShipmentImage> findByShipmentIdAndType(Long shipmentId, ShipmentImageType type);

	/**
	 * 배송의 PICKUP 사진 존재 여부
	 */
	@Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ShipmentImage i WHERE i.shipment.id = :shipmentId AND i.type = 'PICKUP'")
	boolean existsPickupImage(@Param("shipmentId") Long shipmentId);

	/**
	 * 배송의 RECEIPT 사진 존재 여부
	 */
	@Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END FROM ShipmentImage i WHERE i.shipment.id = :shipmentId AND i.type = 'RECEIPT'")
	boolean existsReceiptImage(@Param("shipmentId") Long shipmentId);

	/**
	 * 배송의 사진 개수 조회
	 */
	long countByShipmentId(Long shipmentId);
}