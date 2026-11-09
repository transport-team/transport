package com.transport.service;

import java.util.Optional;

import com.transport.dto.CustomerAddressRequestDTO;
import com.transport.dto.CustomerAddressResponseDTO;

public interface CustomerAddressService {

	Optional<CustomerAddressResponseDTO> getDefaultAddress(Long memberId);

	CustomerAddressResponseDTO saveDefaultAddress(Long memberId, CustomerAddressRequestDTO requestDTO);

	void deleteDefaultAddress(Long memberId);
}
