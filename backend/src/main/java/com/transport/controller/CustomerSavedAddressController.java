package com.transport.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.CustomerSavedAddressRequestDTO;
import com.transport.dto.CustomerSavedAddressResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.CustomerSavedAddressService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 고객 주소록: 주소 한 곳씩 별명과 함께 여러 개 저장하고, 본인 것만 수정·삭제할 수 있다.
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members/me/saved-addresses")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerSavedAddressController {

    private final CustomerSavedAddressService savedAddressService;

    @GetMapping
    public List<CustomerSavedAddressResponseDTO> list(@AuthenticationPrincipal LoginMember loginMember) {
        return savedAddressService.list(loginMember.getId());
    }

    @PostMapping
    public ResponseEntity<CustomerSavedAddressResponseDTO> create(
            @AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody CustomerSavedAddressRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(savedAddressService.create(loginMember.getId(), request));
    }

    @PutMapping("/{id}")
    public CustomerSavedAddressResponseDTO update(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable Long id,
            @Valid @RequestBody CustomerSavedAddressRequestDTO request) {
        return savedAddressService.update(loginMember.getId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal LoginMember loginMember, @PathVariable Long id) {
        savedAddressService.delete(loginMember.getId(), id);
        return ResponseEntity.noContent().build();
    }
}
