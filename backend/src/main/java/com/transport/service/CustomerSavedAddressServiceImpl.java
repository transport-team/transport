package com.transport.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.transport.domain.CustomerSavedAddress;
import com.transport.domain.Member;
import com.transport.dto.CustomerSavedAddressRequestDTO;
import com.transport.dto.CustomerSavedAddressResponseDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
import com.transport.repository.CustomerSavedAddressRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerSavedAddressServiceImpl implements CustomerSavedAddressService {

    private final CustomerSavedAddressRepository savedAddressRepository;
    private final MemberService memberService;

    @Override
    public List<CustomerSavedAddressResponseDTO> list(Long memberId) {
        return savedAddressRepository.findByMemberIdOrderByIdDesc(memberId).stream()
                .map(CustomerSavedAddressResponseDTO::from)
                .toList();
    }

    @Override
    @Transactional
    public CustomerSavedAddressResponseDTO create(Long memberId, CustomerSavedAddressRequestDTO request) {
        Member member = memberService.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
        CustomerSavedAddress address = CustomerSavedAddress.builder()
                .member(member)
                .label(request.label().trim())
                .address(request.address().trim())
                .detail(trimOptional(request.detail()))
                .build();
        return CustomerSavedAddressResponseDTO.from(savedAddressRepository.save(address));
    }

    @Override
    @Transactional
    public CustomerSavedAddressResponseDTO update(Long memberId, Long id, CustomerSavedAddressRequestDTO request) {
        CustomerSavedAddress address = ownAddress(memberId, id);
        address.update(request.label().trim(), request.address().trim(), trimOptional(request.detail()));
        return CustomerSavedAddressResponseDTO.from(address);
    }

    @Override
    @Transactional
    public void delete(Long memberId, Long id) {
        savedAddressRepository.delete(ownAddress(memberId, id));
    }

    private CustomerSavedAddress ownAddress(Long memberId, Long id) {
        // 주소 ID만으로 조회하지 않고 반드시 로그인한 회원 ID까지 함께 확인한다.
        return savedAddressRepository.findByIdAndMemberId(id, memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.NOT_FOUND));
    }

    private String trimOptional(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
