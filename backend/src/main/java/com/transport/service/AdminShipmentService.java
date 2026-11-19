// AdminShipmentService.java (com.transport.service)
package com.transport.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Shipment;
import com.transport.dto.AdminShipmentResponseDTO;
import com.transport.dto.AdminShipmentSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminShipmentService {

	private final ShipmentRepository shipmentRepository;

	public PageResponseDTO<AdminShipmentResponseDTO> searchShipments(AdminShipmentSearchDTO searchDTO) {
		LocalDateTime fromDateTime = (searchDTO.getFrom() != null) ? searchDTO.getFrom().atStartOfDay() : null;
		LocalDateTime toDateTime = (searchDTO.getTo() != null) ? searchDTO.getTo().plusDays(1).atStartOfDay() : null;
		String keyword = (searchDTO.getKeyword() != null) ? searchDTO.getKeyword().trim() : null;

		Page<Shipment> result = shipmentRepository.searchAdminShipments(
				searchDTO.getStatus(),
				fromDateTime,
				toDateTime,
				keyword,
				searchDTO.getPageable(Sort.by("id").descending()));

		List<AdminShipmentResponseDTO> dtoList = result.getContent().stream()
				.map(AdminShipmentResponseDTO::from)
				.toList();

		return PageResponseDTO.<AdminShipmentResponseDTO>withAll()
				.dtoList(dtoList)
				.pageRequestDTO(searchDTO)
				.totalCount(result.getTotalElements())
				.build();
	}
}