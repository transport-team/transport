package com.transport.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import com.transport.dto.DriverDayOffRequestDTO;
import com.transport.dto.DriverWorkHoursRequestDTO;
import com.transport.dto.DriverWorkHoursResponseDTO;
import com.transport.security.LoginMember;
import com.transport.service.DriverDayOffService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

// 기사: 휴무일 조회·등록·취소, 근무 가능 시간 조회·등록·삭제 API (DRV-01)
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/driver/schedules")
@PreAuthorize("hasRole('DRIVER')")
public class DriverDayOffController {

    private final DriverDayOffService driverDayOffService;

    // 월별 휴무일 목록
    @GetMapping("/off")
    public ResponseEntity<List<LocalDate>> getOffDays(
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestParam int year,
            @RequestParam int month) {

        return ResponseEntity.ok(
                driverDayOffService.getOffDays(
                        loginMember.getId(), year, month));
    }

    // 휴무일 등록
    @PostMapping("/off")
    public ResponseEntity<Void> registerOffDay(
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestBody DriverDayOffRequestDTO request) {

        driverDayOffService.registerOffDay(
                loginMember.getId(), request.date());

        return ResponseEntity.noContent().build();
    }

    // 휴무일 취소
    @DeleteMapping("/off/{date}")
    public ResponseEntity<Void> cancelOffDay(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        driverDayOffService.cancelOffDay(loginMember.getId(), date);

        return ResponseEntity.noContent().build();
    }

    // 월별 근무 가능 시간 목록
    @GetMapping("/work")
    public ResponseEntity<List<DriverWorkHoursResponseDTO>> getWorkHours(
            @AuthenticationPrincipal LoginMember loginMember,
            @RequestParam int year,
            @RequestParam int month) {

        return ResponseEntity.ok(
                driverDayOffService.getWorkHours(
                        loginMember.getId(), year, month));
    }

    // 근무 가능 시간 등록 · 수정 (그 날짜에 이미 있으면 바꾼다)
    @PutMapping("/work/{date}")
    public ResponseEntity<DriverWorkHoursResponseDTO> saveWorkHours(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,
            @Valid @RequestBody DriverWorkHoursRequestDTO request) {

        return ResponseEntity.ok(
                driverDayOffService.saveWorkHours(
                        loginMember.getId(), date, request.startTime(), request.endTime()));
    }

    // 근무 가능 시간 삭제
    @DeleteMapping("/work/{date}")
    public ResponseEntity<Void> deleteWorkHours(
            @AuthenticationPrincipal LoginMember loginMember,
            @PathVariable
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date) {

        driverDayOffService.deleteWorkHours(loginMember.getId(), date);

        return ResponseEntity.noContent().build();
    }
}
