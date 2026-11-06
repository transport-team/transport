package com.transport.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 4주차 목 구현 — 5주차에 팀원 2 실구현으로 교체
@Service
@Profile("!test")
public class DispatchServiceMock implements DispatchService {

	// shipmentId -> driverId (임시 데이터, 실제로는 Dispatch 엔티티에서 조회)
	private final Map<Long, Long> dispatchMap = new ConcurrentHashMap<>();

	public void setDriver(Long shipmentId, Long driverId) {
		dispatchMap.put(shipmentId, driverId);
	}

	@Override
	public Long getActiveDriver(Long shipmentId) {
		return dispatchMap.get(shipmentId);
	}
}