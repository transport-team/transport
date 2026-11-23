package com.transport.service;

import com.transport.dto.AdminSettlementMonthResponseDTO;
import com.transport.dto.AdminSettlementDifferenceDTO;
import com.transport.dto.AdminSettlementTrendDTO;
import java.util.List;

public interface AdminSettlementService {
    AdminSettlementMonthResponseDTO getMonthlySettlements(String month);

    // 마감 대상은 이전 달까지. 성공하면 해당 월의 금액은 이후 변경 불가.
    AdminSettlementMonthResponseDTO closeMonth(String month);

    List<AdminSettlementTrendDTO> getMonthlyTrend(String endMonth);

    // 상세 차이 조회는 읽기 전용. 확정 정산 금액에 영향을 주지 않는다.
    AdminSettlementDifferenceDTO getDifferences(String month, Long driverId);

    // 은행 송금은 하지 않고, 실제 송금 후 관리자와 확인 근거를 남긴다.
    AdminSettlementMonthResponseDTO.Item markPaid(Long settlementId, String adminEmail, String transferReference);
}
