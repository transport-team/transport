package com.transport.dto;

import com.transport.domain.CustomerAddress;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CustomerAddressResponseDTO {

	private Long id;
	private String label;
	private String originAddress;
	private String originDetail;
	private String destAddress;
	private String destDetail;

	public static CustomerAddressResponseDTO from(CustomerAddress address) {
		return CustomerAddressResponseDTO.builder()
				.id(address.getId())
				.label(address.getLabel())
				.originAddress(address.getOriginAddress())
				.originDetail(address.getOriginDetail())
				.destAddress(address.getDestAddress())
				.destDetail(address.getDestDetail())
				.build();
	}
}
