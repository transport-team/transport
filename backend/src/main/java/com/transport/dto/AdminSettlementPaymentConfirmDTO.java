package com.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// 실제 계좌이체 내역을 관리자 본인이 확인한 뒤 확인번호/증빙 문구를 입력한다.
// 이 API는 은행에 송금을 요청하지 않는다.
public record AdminSettlementPaymentConfirmDTO(
        @NotBlank(message = "은행 송금 확인번호 또는 확인 메모를 입력해 주세요.")
        @Size(max = 100, message = "송금 확인 내용은 100자 이하로 입력해 주세요.")
        String transferReference) { }
