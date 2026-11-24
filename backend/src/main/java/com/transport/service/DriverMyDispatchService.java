package com.transport.service;

import java.util.List;

import com.transport.dto.DriverAssignedDispatchResponseDTO;
import com.transport.dto.DriverDispatchHistoryResponseDTO;

public interface DriverMyDispatchService {

    List<DriverAssignedDispatchResponseDTO> getMyDispatches(Long memberId);

    DriverAssignedDispatchResponseDTO getMyDispatch(Long memberId, Long shipmentId);

    List<DriverDispatchHistoryResponseDTO> getMyHistory(Long memberId);

    DriverDispatchHistoryResponseDTO getMyHistoryDetail(Long memberId, Long applicationId);
}
