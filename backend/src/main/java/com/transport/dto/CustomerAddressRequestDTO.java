package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class CustomerAddressRequestDTO {

	@NotBlank(message = "배송지 이름을 입력해 주세요.")
	@Size(max = 50, message = "배송지 이름은 50자 이하로 입력해 주세요.")
	private String label;

	@NotBlank(message = "출발지 주소를 입력해 주세요.")
	private String originAddress;

	private String originDetail;

	@NotBlank(message = "도착지 주소를 입력해 주세요.")
	private String destAddress;

	private String destDetail;
}
