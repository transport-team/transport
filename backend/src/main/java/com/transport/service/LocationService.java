package com.transport.service;

import com.transport.domain.enums.MemberRole;
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
	 * 권한: 관리자 · 본인 고객 · 배정 기사만 (아니면 FORBIDDEN)
	 * 운송중(IN_TRANSIT)이 아니거나 기록이 없으면 null (404 아님 — 보여 줄 위치가 없는 정상 상태)
	 * @param memberId 로그인 회원 ID
	 * @param role 로그인 회원 역할
	 */
	LocationResponseDTO getLatestLocation(Long shipmentId, Long memberId, MemberRole role);
}