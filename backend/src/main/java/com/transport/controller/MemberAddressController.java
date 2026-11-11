package com.transport.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.CustomerAddressResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.CustomerAddressService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members/me")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()") // 로그인한 사용자만 접근 가능
public class MemberAddressController {

    private final CustomerAddressService customerAddressService;

    @GetMapping("/recent-address")
    public ResponseEntity<List<CustomerAddressResponseDTO>> getRecentAddress(
            @AuthenticationPrincipal LoginMember loginMember // 👈 알아낸 타입 적용!
    ) {
        // 1. 현재 로그인한 사용자의 ID 가져오기
        Long memberId = loginMember.getId();
        
        // 2. 서비스에서 최근 주소 목록 조회
        List<CustomerAddressResponseDTO> recentAddresses = customerAddressService.findRecentAddressesByMemberId(memberId);
        
        // 3. 프론트엔드로 반환
        return ResponseEntity.ok(recentAddresses);
    }
}