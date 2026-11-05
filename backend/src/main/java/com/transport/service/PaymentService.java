package com.transport.service;

import com.transport.dto.PaymentApproveRequestDTO;

public interface PaymentService {
    void verifyAndApprovePayment(PaymentApproveRequestDTO requestDTO);
}