package com.transport.service;

public interface AdminDispatchCancellationService {
    // 배차 해제 후 결제된 배송은 다시 배차 대기로 변경 (주문/결제 취소 아님)
    void cancelDispatch(Long shipmentId, Long adminMemberId, String reason);
}
