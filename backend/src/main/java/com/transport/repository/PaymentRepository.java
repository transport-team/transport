package com.transport.repository;

import com.transport.domain.Payment;
import com.transport.domain.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    // 토스페이먼츠 결제 승인 시 고유 주문번호로 결제 내역을 찾기 위한 메서드
    Optional<Payment> findByOrderId(String orderId);

    // 결제 승인 동시 요청이 같은 Shipment 버전을 갱신하지 않도록 결제와 배송을 함께 잠근다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p join fetch p.shipment where p.orderId = :orderId")
    Optional<Payment> findByOrderIdForUpdate(@Param("orderId") String orderId);

    // ===== 관리자 대시보드 집계 (기간은 paidAt 기준 [from, to) — from 이상, to 미만) =====

    // 기간 안 건수 · 금액 합계 (결제가 없으면 0, 0)
    @Query("select count(p) as paidCount, coalesce(sum(p.amount), 0) as revenue from Payment p "
            + "where p.status = :status and p.paidAt >= :from and p.paidAt < :to")
    PaidSum sumByPaidAt(@Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 날짜별 건수 · 금액 합계 (결제가 있는 날만, 날짜 순)
    @Query("select cast(p.paidAt as LocalDate) as paidDate, count(p) as paidCount, sum(p.amount) as revenue from Payment p "
            + "where p.status = :status and p.paidAt >= :from and p.paidAt < :to "
            + "group by cast(p.paidAt as LocalDate) order by cast(p.paidAt as LocalDate)")
    List<DailyPaidSum> sumGroupByPaidDate(@Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 배송의 요청 차종별 건수 · 금액 합계 (결제가 있는 차종만, 차종 id 순)
    @Query("select vt.id as vehicleTypeId, vt.name as vehicleTypeName, count(p) as paidCount, sum(p.amount) as revenue "
            + "from Payment p join p.shipment s join s.vehicleType vt "
            + "where p.status = :status and p.paidAt >= :from and p.paidAt < :to "
            + "group by vt.id, vt.name order by vt.id")
    List<VehicleTypePaidSum> sumGroupByVehicleType(@Param("status") PaymentStatus status,
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    // 기간 합계 결과
    interface PaidSum {
        Long getPaidCount();

        Long getRevenue();
    }

    // 날짜별 합계 결과
    interface DailyPaidSum {
        LocalDate getPaidDate();

        Long getPaidCount();

        Long getRevenue();
    }

    // 차종별 합계 결과
    interface VehicleTypePaidSum {
        Long getVehicleTypeId();

        String getVehicleTypeName();

        Long getPaidCount();

        Long getRevenue();
    }
}
