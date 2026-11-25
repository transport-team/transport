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
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.ShipmentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminShipmentService {

	private final ShipmentRepository shipmentRepository;

	public PageResponseDTO<AdminShipmentResponseDTO> searchShipments(AdminShipmentSearchDTO searchDTO) {
		// 기간을 거꾸로 넣으면 400 (전에는 200에 빈 목록 — 기사 정산 조회와 같게, #108 댓글)
		if (searchDTO.getFrom() != null && searchDTO.getTo() != null && searchDTO.getFrom().isAfter(searchDTO.getTo())) {
			throw new CustomException(ErrorCode.INVALID_INPUT, "검색 시작일이 종료일보다 늦어요. 기간을 다시 선택해 주세요.");
		}
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