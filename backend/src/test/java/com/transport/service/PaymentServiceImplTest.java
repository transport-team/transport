package com.transport.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.transport.client.TossPaymentClient;
import com.transport.domain.Member;
import com.transport.domain.Payment;
import com.transport.domain.Shipment;
import com.transport.domain.enums.MemberRole;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.PaymentApproveRequestDTO;
import com.transport.repository.PaymentRepository;

import jakarta.persistence.EntityManager;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TossPaymentClient tossPaymentClient;

    @Mock
    private StatusHistoryService statusHistoryService;

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(paymentService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("토스 승인 후 DB 저장 실패 시 토스 결제를 취소하고 원래 예외를 전파한다")
    void verifyAndApprovePayment_cancelsTossPaymentWhenFlushFails() {
        String orderId = "order-1";
        String paymentKey = "payment-key";
        int amount = 10000;
        Payment payment = pendingPayment(orderId, amount);
        PaymentApproveRequestDTO request = request(orderId, paymentKey, amount);

        when(paymentRepository.findByOrderIdForUpdate(orderId)).thenReturn(Optional.of(payment));
        doThrow(new DataIntegrityViolationException("DB 제약조건 위반"))
                .when(entityManager).flush();

        assertThatThrownBy(() -> paymentService.verifyAndApprovePayment(request, 10L))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(tossPaymentClient).confirm(paymentKey, orderId, amount);
        verify(tossPaymentClient).cancelPayment(eq(paymentKey), anyString());
    }

    private Payment pendingPayment(String orderId, int amount) {
        Member customer = Member.builder()
                .id(10L)
                .role(MemberRole.CUSTOMER)
                .build();
        Shipment shipment = Shipment.builder()
                .customer(customer)
                .fare(amount)
                .status(ShipmentStatus.PENDING_PAYMENT)
                .pickupAt(LocalDateTime.now().plusDays(1))
                .build();
        return Payment.builder()
                .shipment(shipment)
                .orderId(orderId)
                .amount(amount)
                .status(PaymentStatus.READY)
                .build();
    }

    private PaymentApproveRequestDTO request(String orderId, String paymentKey, int amount) {
        PaymentApproveRequestDTO request = new PaymentApproveRequestDTO();
        ReflectionTestUtils.setField(request, "orderId", orderId);
        ReflectionTestUtils.setField(request, "paymentKey", paymentKey);
        ReflectionTestUtils.setField(request, "amount", amount);
        return request;
    }
}
