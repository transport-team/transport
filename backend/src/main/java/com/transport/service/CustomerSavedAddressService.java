package com.transport.service;

import java.util.List;

import com.transport.dto.CustomerSavedAddressRequestDTO;
import com.transport.dto.CustomerSavedAddressResponseDTO;

public interface CustomerSavedAddressService {

    List<CustomerSavedAddressResponseDTO> list(Long memberId);

    CustomerSavedAddressResponseDTO create(Long memberId, CustomerSavedAddressRequestDTO request);

    CustomerSavedAddressResponseDTO update(Long memberId, Long id, CustomerSavedAddressRequestDTO request);

    void delete(Long memberId, Long id);
}
