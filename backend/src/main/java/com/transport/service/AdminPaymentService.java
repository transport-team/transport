// AdminPaymentService.java (com.transport.service)
package com.transport.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.Payment;
import com.transport.dto.AdminPaymentResponseDTO;
import com.transport.dto.AdminPaymentSearchDTO;
import com.transport.dto.PageResponseDTO;
import com.transport.repository.PaymentRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminPaymentService {

	private final PaymentRepository paymentRepository;

	public PageResponseDTO<AdminPaymentResponseDTO> searchPayments(AdminPaymentSearchDTO searchDTO) {
		LocalDateTime fromDateTime = (searchDTO.getFrom() != null) ? searchDTO.getFrom().atStartOfDay() : null;
		LocalDateTime toDateTime = (searchDTO.getTo() != null) ? searchDTO.getTo().plusDays(1).atStartOfDay() : null;
		String keyword = (searchDTO.getKeyword() != null) ? searchDTO.getKeyword().trim() : null;

		Page<Payment> result = paymentRepository.searchAdminPayments(
				searchDTO.getStatus(),
				fromDateTime,
				toDateTime,
				keyword,
				searchDTO.getPageable(Sort.by("id").descending()));

		List<AdminPaymentResponseDTO> dtoList = result.getContent().stream()
				.map(AdminPaymentResponseDTO::from)
				.toList();

		return PageResponseDTO.<AdminPaymentResponseDTO>withAll()
				.dtoList(dtoList)
				.pageRequestDTO(searchDTO)
				.totalCount(result.getTotalElements())
				.build();
	}
}