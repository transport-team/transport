package com.transport.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.transport.domain.SettlementPeriod;

public interface SettlementPeriodRepository extends JpaRepository<SettlementPeriod, String> {
}
