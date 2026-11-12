package com.transport.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.repository.DispatchRepository;

import lombok.RequiredArgsConstructor;

// 실제 DB에서 배정된 기사를 조회하는 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchServiceImpl implements DispatchService {

    private final DispatchRepository dispatchRepository;

    @Override
    public Long getActiveDriver(Long shipmentId) {

        // 1. 배송 ID로 현재 활성화된 배차 조회
        return dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(shipmentId)

                // 2. 배차가 있다면 기사 회원 ID 반환
                .map(dispatch -> dispatch.getDriver().getMember().getId())

                // 3. 활성 배차가 없다면 null 반환
                .orElse(null);
    }
}
