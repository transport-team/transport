
package com.transport.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Dispatch;
import com.transport.repository.DispatchRepository;

import lombok.RequiredArgsConstructor;

// 실제 DB에서 현재 배정된 기사를 조회하는 서비스
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DispatchServiceImpl implements DispatchService {

    private final DispatchRepository dispatchRepository;

    @Override
    public Long getActiveDriver(Long shipmentId) {

        // 배송 ID로 최근 활성 배차 1건 조회
        return dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(shipmentId)

                // 기사 정보 ID가 아닌 회원 ID 반환
                .map(dispatch -> dispatch.getDriver().getMember().getId())

                // 활성 배차가 없으면 null 반환
                .orElse(null);
    }

    @Override
    @Transactional
    public void releaseDispatch(Long shipmentId) {
        // 활성 배차가 없으면 이미 해제된 것으로 처리
        dispatchRepository
                .findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(shipmentId)
                .ifPresent(Dispatch::release);
    }

    @Override
    public java.util.Optional<Dispatch> findActiveDispatch(Long shipmentId) {
        return dispatchRepository.findFirstByShipment_IdAndActiveTrueOrderByAssignedAtDesc(shipmentId);
    }
}
