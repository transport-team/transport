package com.transport.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Driver;
import com.transport.domain.LocationLog;
import com.transport.domain.Shipment;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.LocationResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.LocationLogRepository;

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
	// 다른 도메인은 약속 메서드로만 (배송 = 팀원 3, 기사 = 팀원 2 — 저장소 직접 사용 정리, #94)
	private final ShipmentService shipmentService;
	private final DriverService driverService;
	private final NotificationServiceImpl notificationService; // SSE emitter 저장소 재사용 (getEmitter)
	private final DispatchService dispatchService;             // 배정 기사 회원 ID (약속된 getActiveDriver)

	@Override
	public LocationResponseDTO sendLocation(Long shipmentId, Long memberId, double lat, double lng) {

		// 1. 배송 조회 (없으면 SHIPMENT_NOT_FOUND — ShipmentService 약속 메서드)
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 2. 로그인 기사의 Driver 엔티티 (위치 기록에 기사를 남긴다)
		Driver driver = driverService.findByMemberId(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.FORBIDDEN, "기사만 위치를 전송할 수 있습니다."));

		// 3. 배정 기사인지 — getActiveDriver는 배정 기사의 회원 ID라 로그인 회원 ID와 바로 비교한다
		Long activeDriverMemberId = dispatchService.getActiveDriver(shipmentId);
		if (activeDriverMemberId == null || !activeDriverMemberId.equals(memberId)) {
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

		// 6. 이 배송의 고객이 SSE로 접속 중이면 열어 둔 모든 탭에 location 이벤트 (접속 중이 아니면 저장분만)
		//    전송에 실패한 탭은 NotificationServiceImpl이 정리하고, 저장은 유지한다 (다음 위치가 30초 뒤에 온다)
		notificationService.sendToMember(shipment.getCustomer().getId(), "location",
				"{\"shipmentId\":" + shipmentId
						+ ",\"lat\":" + response.getLat()
						+ ",\"lng\":" + response.getLng()
						+ ",\"recordedAt\":\"" + response.getRecordedAt() + "\"}");

		return response;
	}

	@Override
	@Transactional(readOnly = true)
	public LocationResponseDTO getLatestLocation(Long shipmentId, Long memberId, MemberRole role) {
		Shipment shipment = shipmentService.getShipment(shipmentId);

		// 권한: 관리자 · 본인 고객 · 배정 기사만 (이력 조회 StatusServiceImpl.getHistory와 같은 규칙)
		boolean isAdmin = role == MemberRole.ADMIN;
		boolean isOwner = shipment.getCustomer() != null
				&& shipment.getCustomer().getId().equals(memberId);
		Long assignedDriverId = dispatchService.getActiveDriver(shipmentId); // 배정 기사의 회원 ID
		boolean isAssignedDriver = assignedDriverId != null && assignedDriverId.equals(memberId);
		if (!isAdmin && !isOwner && !isAssignedDriver) {
			throw new CustomException(ErrorCode.FORBIDDEN, "본인 배송의 위치만 볼 수 있습니다.");
		}

		// 운송중(IN_TRANSIT)일 때만 보여 준다 — 개인정보: 운송 전 · 끝난 뒤에는 위치를 보여주지 않는다
		if (shipment.getStatus() != ShipmentStatus.IN_TRANSIT) {
			return null;
		}

		return locationLogRepository.findTopByShipmentIdOrderByRecordedAtDesc(shipmentId)
				.map(LocationResponseDTO::from)
				.orElse(null); // 아직 위치 기록이 없으면 null (정상)
	}
}