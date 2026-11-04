package com.transport.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.repository.ShipmentStatusHistoryRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

// 상태 변경 이력 저장 구현 (팀원 4)
@Service
@Transactional
@Log4j2
@RequiredArgsConstructor
public class StatusHistoryServiceImpl implements StatusHistoryService {

	private final ShipmentStatusHistoryRepository historyRepository;

	@Override
	public ShipmentStatusHistory record(Shipment shipment, ShipmentStatus from, ShipmentStatus to, Member changedBy, String memo) {
		ShipmentStatusHistory history = ShipmentStatusHistory.builder()
				.shipment(shipment)
				.fromStatus(from)
				.toStatus(to)
				.changedBy(changedBy)
				.memo(memo)
				.build();

		ShipmentStatusHistory saved = historyRepository.save(history);

		log.info("배송 상태 이력 저장: shipmentId={}, {} -> {}, changedBy={}", shipment.getId(), from, to, changedBy != null ? changedBy.getId() : "system");
		return saved;
	}
}