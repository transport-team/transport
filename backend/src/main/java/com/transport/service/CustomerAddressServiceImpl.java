package com.transport.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CustomerAddress;
import com.transport.domain.Member;
import com.transport.dto.CustomerAddressRequestDTO;
import com.transport.dto.CustomerAddressResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.CustomerAddressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomerAddressServiceImpl implements CustomerAddressService {

	private final CustomerAddressRepository customerAddressRepository;
	private final MemberService memberService;

	@Override
	@Transactional(readOnly = true)
	public Optional<CustomerAddressResponseDTO> getDefaultAddress(Long memberId) {
		return customerAddressRepository.findByMemberId(memberId)
				.map(CustomerAddressResponseDTO::from);
	}

	@Override
	public CustomerAddressResponseDTO saveDefaultAddress(Long memberId, CustomerAddressRequestDTO requestDTO) {
		Member member = memberService.findById(memberId)
				.orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
		CustomerAddress address = customerAddressRepository.findByMemberId(memberId)
				.map(existing -> {
					existing.update(requestDTO.getLabel(), requestDTO.getOriginAddress(), requestDTO.getOriginDetail(),
							requestDTO.getDestAddress(), requestDTO.getDestDetail());
					return existing;
				})
				.orElseGet(() -> CustomerAddress.builder()
						.member(member)
						.label(requestDTO.getLabel())
						.originAddress(requestDTO.getOriginAddress())
						.originDetail(requestDTO.getOriginDetail())
						.destAddress(requestDTO.getDestAddress())
						.destDetail(requestDTO.getDestDetail())
						.build());

		return CustomerAddressResponseDTO.from(customerAddressRepository.save(address));
	}

	@Override
	public void deleteDefaultAddress(Long memberId) {
		customerAddressRepository.findByMemberId(memberId)
				.ifPresent(customerAddressRepository::delete);
	}
}
