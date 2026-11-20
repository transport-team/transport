package com.transport.service;

import java.time.LocalDate;
import java.util.List;

import com.transport.dto.DriverMonthlySettlementResponseDTO;
import com.transport.dto.DriverSettlementItemResponseDTO;
import com.transport.dto.DriverSettlementRangeResponseDTO;

public interface DriverSettlementService {

    DriverMonthlySettlementResponseDTO getMonthlySummary(Long memberId, String settleMonth);

    List<DriverSettlementItemResponseDTO> getMonthlyItems(Long memberId, String settleMonth);

    DriverSettlementRangeResponseDTO getRangeSummary(Long memberId, LocalDate from, LocalDate to);

    List<DriverSettlementItemResponseDTO> getRangeItems(Long memberId, LocalDate from, LocalDate to);
}
