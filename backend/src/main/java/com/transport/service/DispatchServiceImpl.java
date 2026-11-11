package com.transport.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.repository.DispatchRepository;
import com.transport.repository.DriverRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 배차 서비스 실구현 (5-5 — 팀원 2 소유를 팀원 4가 임시 작성, PR에서 리뷰 요청)
// getActiveDriver: 배송의 현재 유효한(active) 배차 → 기사 → 회원 ID(member.id) 반환
// ※ 약속 표 5주차: driver.id가 아니라 member.id를 반환한다 (상태 변경 · 이력 조회가 로그인 회원 ID와 비교)
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Log4j2
public class DispatchServiceImpl implements DispatchService {

	private final DispatchRepository dispatchRepository;
	private final DriverRepository driverRepository;

	@Override
	public Long getActiveDriver(Long shipmentId) {
		return dispatchRepository.findByShipmentIdAndActiveTrue(shipmentId)
				.map(dispatch -> dispatch.getDriver())
				.map(driver -> driver.getMember())
				.map(member -> member.getId())
				.orElse(null); // 배차가 없으면 null (호출 쪽에서 NOT_ASSIGNED_DRIVER 처리)
	}
}