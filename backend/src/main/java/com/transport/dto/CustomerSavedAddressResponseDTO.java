package com.transport.dto;

import com.transport.domain.CustomerSavedAddress;

public record CustomerSavedAddressResponseDTO(
        Long id,
        String label,
        String address,
        String detail) {

    public static CustomerSavedAddressResponseDTO from(CustomerSavedAddress savedAddress) {
        return new CustomerSavedAddressResponseDTO(
                savedAddress.getId(), savedAddress.getLabel(),
                savedAddress.getAddress(), savedAddress.getDetail());
    }
}
