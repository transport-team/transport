package com.transport.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.CustomerAddressRequestDTO;
import com.transport.dto.CustomerAddressResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.CustomerAddressService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Customer Address API", description = "고객 기본 배송지 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/default-address")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerAddressController {

	private final CustomerAddressService customerAddressService;

	@Operation(summary = "기본 배송지 조회")
	@GetMapping
	public ResponseEntity<CustomerAddressResponseDTO> getDefaultAddress(
			@AuthenticationPrincipal LoginMember loginMember) {
		return customerAddressService.getDefaultAddress(loginMember.getId())
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@Operation(summary = "기본 배송지 저장 또는 수정")
	@PutMapping
	public ResponseEntity<CustomerAddressResponseDTO> saveDefaultAddress(
			@AuthenticationPrincipal LoginMember loginMember,
			@Valid @RequestBody CustomerAddressRequestDTO requestDTO) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(customerAddressService.saveDefaultAddress(loginMember.getId(), requestDTO));
	}

	@Operation(summary = "기본 배송지 삭제")
	@DeleteMapping
	public ResponseEntity<Void> deleteDefaultAddress(@AuthenticationPrincipal LoginMember loginMember) {
		customerAddressService.deleteDefaultAddress(loginMember.getId());
		return ResponseEntity.noContent().build();
	}
}
