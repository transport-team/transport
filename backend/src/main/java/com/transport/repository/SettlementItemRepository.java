package com.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.transport.domain.SettlementItem;

public interface SettlementItemRepository extends JpaRepository<SettlementItem, Long> {
    boolean existsByShipmentId(Long shipmentId);
    List<SettlementItem> findBySettlement_Id(Long settlementId);
}
