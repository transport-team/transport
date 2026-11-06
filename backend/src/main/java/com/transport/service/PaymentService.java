package com.transport.service;

import com.transport.dto.DailyStatDTO;
import com.transport.dto.PaidStatDTO;
import com.transport.dto.PaymentApproveRequestDTO;
import com.transport.dto.VehicleTypeStatDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface PaymentService {
    void verifyAndApprovePayment(PaymentApproveRequestDTO requestDTO);

    // ===== 관리자 대시보드 집계: 결제 완료(PAID)만, paidAt 기준 (환불돼 CANCELED가 된 결제는 빠진다) =====

    // 기간 [from, to) 결제 완료 건수 · 금액 합계
    PaidStatDTO getPaidStat(LocalDateTime from, LocalDateTime to);

    // fromDate ~ toDate(포함) 날짜별 결제 완료 건수 · 금액 (결제가 있는 날만, 날짜 순)
    List<DailyStatDTO> getDailyPaidStats(LocalDate fromDate, LocalDate toDate);

    // 기간 [from, to) 배송의 요청 차종별 결제 완료 건수 · 금액 (결제가 있는 차종만, 차종 id 순)
    List<VehicleTypeStatDTO> getPaidStatsByVehicleType(LocalDateTime from, LocalDateTime to);
}
