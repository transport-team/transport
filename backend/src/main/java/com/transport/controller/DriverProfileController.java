package com.transport.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.transport.dto.DriverDTO;
import com.transport.dto.DriverProfileUpdateRequestDTO;
import com.transport.security.LoginMember;
import com.transport.service.DriverService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 기사 본인 정보: URL의 기사 ID가 아닌 로그인한 회원 ID로 조회/수정한다.
@RestController
@RequestMapping("/api/drivers/me")
@RequiredArgsConstructor
@PreAuthorize("hasRole('DRIVER')")
public class DriverProfileController {

    private final DriverService driverService;

    @GetMapping
    public DriverDTO getMyProfile(@AuthenticationPrincipal LoginMember loginMember) {
        return driverService.getMyProfile(loginMember.getId());
    }

    @PutMapping
    public DriverDTO updateMyProfile(@AuthenticationPrincipal LoginMember loginMember,
            @Valid @RequestBody DriverProfileUpdateRequestDTO request) {
        return driverService.updateMyProfile(loginMember.getId(), request);
    }
}
