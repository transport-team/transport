package com.transport.service;

import com.transport.dto.LocationResponseDTO;

// 위치 서비스 (7-1 — 팀원 4)
public interface LocationService {

	/**
	 * 위치 전송 (배정 기사 + IN_TRANSIT만)
	 * @param shipmentId 배송 ID
	 * @param memberId 로그인 기사 회원 ID
	 * @param lat 위도
	 * @param lng 경도
	 * @return 저장된 위치 (SSE 이벤트 본문과 같은 모양)
	 */
	LocationResponseDTO sendLocation(Long shipmentId, Long memberId, double lat, double lng);

	/**
	 * 마지막 위치 (고객 상세 지도 초기 위치)
	 * 기록이 없으면 null (404 아님 — 아직 위치를 안 보낸 건 정상 상태)
	 */
	LocationResponseDTO getLatestLocation(Long shipmentId);
}