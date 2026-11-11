package com.transport.service;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.transport.domain.Driver;
import com.transport.domain.LocationLog;
import com.transport.domain.Shipment;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.LocationResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;
import com.transport.repository.LocationLogRepository;
import com.transport.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 위치 서비스 구현 (7-1 — 팀원 4)
// 알림(6-1)과의 차이: 위치는 '저장 성공 = 곧바로 다음 위치가 옴'이라 커밋 후 이벤트로 감싸지 않고
// 트랜잭션 안에서 emitter에 직접 보낸다. 전송 실패해도 저장은 유지 (30초 뒤 다음 위치가 덮는다).
@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class LocationServiceImpl implements LocationService {

	private final LocationLogRepository locationLogRepository;
	private final ShipmentRepository shipmentRepository;
	private final DispatchRepository dispatchRepository;
	private final DriverRepository driverRepository;
	private final NotificationServiceImpl notificationService; // SSE emitter 저장소 재사용 (getEmitter)

	@Override
	public LocationResponseDTO sendLocation(Long shipmentId, Long memberId, double lat, double lng) {

		// 1. 배송 조회
		Shipment shipment = shipmentRepository.findById(shipmentId)
				.orElseThrow(() -> new CustomException(ErrorCode.SHIPMENT_NOT_FOUND));

		// 2. 로그인 기사의 Driver 엔티티
		Driver driver = driverRepository.findByMemberId(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "기사만 위치를 전송할 수 있습니다."));

		// 3. 배정 기사인지 (dispatch의 driver는 Driver — member.id가 아니라 driver.id와 비교)
		Long activeDriverId = dispatchRepository.findByShipmentIdAndActiveTrue(shipmentId)
				.map(d -> d.getDriver().getId())
				.orElse(null);
		if (activeDriverId == null || !activeDriverId.equals(driver.getId())) {
			throw new CustomException(ErrorCode.NOT_ASSIGNED_DRIVER);
		}

		// 4. 운송중(IN_TRANSIT)일 때만 — 아니면 거부 (개인정보: 운송 중이 아닌 위치는 수집하지 않는다)
		if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
			throw new CustomException(ErrorCode.NOT_IN_TRANSIT);
		}

		// 5. 저장
		LocationLog saved = locationLogRepository.save(LocationLog.builder()
				.shipment(shipment)
				.driver(driver)
				.lat(lat)
				.lng(lng)
				.recordedAt(LocalDateTime.now())
				.build());

		LocationResponseDTO response = LocationResponseDTO.from(saved);

		// 6. 이 배송의 고객이 SSE로 접속 중이면 즉시 location 이벤트 전송 (접속 중이 아니면 저장분만)
		SseEmitter emitter = notificationService.getEmitter(shipment.getCustomer().getId());
		if (emitter != null) {
			try {
				emitter.send(SseEmitter.event()
						.name("location")
						.data("{\"shipmentId\":" + shipmentId
								+ ",\"lat\":" + response.getLat()
								+ ",\"lng\":" + response.getLng()
								+ ",\"recordedAt\":\"" + response.getRecordedAt() + "\"}"));
			} catch (IOException e) {
				// 전송 실패는 무시 — 저장은 유지 (다음 위치가 30초 뒤에 온다)
				log.warn("위치 SSE 전송 실패(저장은 유지): shipmentId={}", shipmentId);
			}
		}

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public LocationResponseDTO getLatestLocation(Long shipmentId) {
		return locationLogRepository.findTopByShipmentIdOrderByRecordedAtDesc(shipmentId)
				.map(LocationResponseDTO::from)
				.orElse(null); // 아직 위치 기록이 없으면 null (정상)
	}
}