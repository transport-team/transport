package com.transport.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Member;
import com.transport.domain.Shipment;
import com.transport.domain.ShipmentStatusHistory;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.DriverCompletedShipmentDTO;
import com.transport.dto.DriverCompletedStatDTO;
import com.transport.dto.DriverCompletedTotalDTO;
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
					.changedAt(java.time.LocalDateTime.now())
					.build();

		ShipmentStatusHistory saved = historyRepository.save(history);

		log.info("배송 상태 이력 저장: shipmentId={}, {} -> {}, changedBy={}", shipment.getId(), from, to, changedBy != null ? changedBy.getId() : "system");
		return saved;
	}

	@Override
	@Transactional(readOnly = true)
	public List<ShipmentStatusHistory> getHistory(Long shipmentId) {
		return historyRepository.findByShipment_IdOrderByChangedAtAsc(shipmentId);
	}

	@Override
	@Transactional(readOnly = true)
	public long countCompleted(LocalDateTime from, LocalDateTime to) {
		return historyRepository.countByToStatusBetween(ShipmentStatus.COMPLETED, from, to);
	}

	@Override
	@Transactional(readOnly = true)
	public List<DriverCompletedStatDTO> getCompletedCountsByDriver(LocalDateTime from, LocalDateTime to, int limit) {
		return historyRepository.countGroupByDriver(ShipmentStatus.COMPLETED, from, to, PageRequest.of(0, limit))
				.stream()
				.map(row -> DriverCompletedStatDTO.builder()
						.driverId(row.getDriverId())
						.driverName(row.getDriverName())
						.completedCount(row.getChangedCount())
						.build())
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public DriverCompletedTotalDTO getDriverCompletedTotal(Long driverMemberId, LocalDateTime from, LocalDateTime to) {
		var total = historyRepository.findDriverMonthlyTotal(driverMemberId, ShipmentStatus.COMPLETED, from, to);
		return new DriverCompletedTotalDTO(
				total.getCompletedCount() == null ? 0 : total.getCompletedCount(),
				total.getTotalFare() == null ? 0 : total.getTotalFare());
	}

	@Override
	@Transactional(readOnly = true)
	public List<DriverCompletedShipmentDTO> getDriverCompletedShipments(Long driverMemberId, LocalDateTime from, LocalDateTime to) {
		return historyRepository.findDriverMonthlyFareItems(driverMemberId, ShipmentStatus.COMPLETED, from, to)
				.stream()
				.map(item -> new DriverCompletedShipmentDTO(
						item.getShipmentId(),
						item.getCargoName(),
						item.getFare() == null ? 0 : item.getFare(),
						item.getCompletedAt()))
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public Optional<LocalDateTime> getLastChangedAt(Long shipmentId, ShipmentStatus toStatus) {
		return historyRepository.findFirstByShipment_IdAndToStatusOrderByChangedAtDesc(shipmentId, toStatus)
				.map(ShipmentStatusHistory::getChangedAt);
	}
}