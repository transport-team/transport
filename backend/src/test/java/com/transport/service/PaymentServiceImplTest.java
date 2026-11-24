package com.transport.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
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
import com.transport.domain.enums.NotificationType;
import com.transport.domain.enums.PaymentStatus;
import com.transport.domain.enums.ShipmentStatus;
import com.transport.dto.PaymentApproveRequestDTO;
import com.transport.exception.CustomException;
import com.transport.exception.ErrorCode;
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

    @Mock
    private DispatchApplicationService dispatchApplicationService;

    @Mock
    private DispatchService dispatchService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MemberService memberService;

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

    // ===== 고객 취소 (#102 후속) =====

    @Test
    @DisplayName("접수(결제 완료) 건 취소: 결제 · 배송 · 이력 · 대기 지원 마감을 먼저 저장(flush)하고 토스 환불은 마지막")
    void cancelPayment_savesFirstThenRefunds() {
        Payment payment = paidPayment(ShipmentStatus.REQUESTED, "toss-key");
        Shipment shipment = payment.getShipment();
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));

        paymentService.cancelPayment(1L, "단순 변심", 10L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELED);
        InOrder order = inOrder(statusHistoryService, dispatchApplicationService, entityManager, tossPaymentClient);
        order.verify(statusHistoryService).record(shipment, ShipmentStatus.REQUESTED, ShipmentStatus.CANCELED,
                shipment.getCustomer(), "고객 취소: 단순 변심");
        order.verify(dispatchApplicationService).closePendingApplications(shipment);
        order.verify(entityManager).flush();
        order.verify(tossPaymentClient).cancelPayment("toss-key", "단순 변심");
    }

    @Test
    @DisplayName("토스 환불이 실패하면 예외가 그대로 나가 트랜잭션이 롤백된다 (DB는 환불 전에 flush)")
    void cancelPayment_refundFailurePropagates() {
        Payment payment = paidPayment(ShipmentStatus.REQUESTED, "toss-key");
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));
        doThrow(new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 취소에 실패했습니다."))
                .when(tossPaymentClient).cancelPayment("toss-key", "단순 변심");

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, "단순 변심", 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_FAILED);
        verify(entityManager).flush();
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = {"ASSIGNED", "PICKED_UP", "IN_TRANSIT", "ARRIVED", "DELIVERED", "COMPLETED", "CANCELED"})
    @DisplayName("배차가 확정된 뒤 · 이미 취소된 배송은 고객이 취소할 수 없고 아무것도 바뀌지 않는다 (409 CANNOT_CANCEL)")
    void cancelPayment_onlyBeforeDispatch(ShipmentStatus status) {
        Payment payment = paidPayment(status, "toss-key");
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, "단순 변심", 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_CANCEL);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verifyNoInteractions(tossPaymentClient, statusHistoryService, dispatchApplicationService);
    }

    @Test
    @DisplayName("배차가 확정된 배송은 관리자에게 취소를 요청하라고 안내한다")
    void cancelPayment_assignedMessage() {
        when(paymentRepository.findByShipmentIdForUpdate(1L))
                .thenReturn(Optional.of(paidPayment(ShipmentStatus.ASSIGNED, "toss-key")));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, "단순 변심", 10L))
                .hasMessage("배차가 확정된 배송은 관리자에게 취소를 요청해 주세요.");
    }

    @Test
    @DisplayName("결제 대기 건과 개발용 결제 건너뛰기 건은 토스를 부르지 않는다")
    void cancelPayment_noRefundCallWithoutTossPayment() {
        Payment ready = pendingPayment("order-2", 10000);
        Payment skipped = paidPayment(ShipmentStatus.REQUESTED, "LOCAL_SKIP_order-3");
        when(paymentRepository.findByShipmentIdForUpdate(2L)).thenReturn(Optional.of(ready));
        when(paymentRepository.findByShipmentIdForUpdate(3L)).thenReturn(Optional.of(skipped));

        paymentService.cancelPayment(2L, "결제 안 함", 10L);
        paymentService.cancelPayment(3L, "점검", 10L);

        assertThat(ready.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(skipped.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        verifyNoInteractions(tossPaymentClient);
    }

    @Test
    @DisplayName("다른 고객의 배송은 취소할 수 없다 (403)")
    void cancelPayment_otherCustomer() {
        when(paymentRepository.findByShipmentIdForUpdate(1L))
                .thenReturn(Optional.of(paidPayment(ShipmentStatus.REQUESTED, "toss-key")));

        assertThatThrownBy(() -> paymentService.cancelPayment(1L, "단순 변심", 99L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.FORBIDDEN);
        verifyNoInteractions(tossPaymentClient, statusHistoryService, dispatchApplicationService);
    }

    // ===== 관리자 취소 (ADM-02, 11-19 결정 34) =====

    private final Member admin = Member.builder().id(1L).role(MemberRole.ADMIN).build();
    private final Member driverMember = Member.builder().id(30L).role(MemberRole.DRIVER).build();

    @Test
    @DisplayName("배차된 결제 건: 결제 · 배송 · 이력 · 대기 지원 마감 · 배차 해제를 먼저 저장하고, 고객 · 기사 알림 뒤 토스 환불은 마지막")
    void cancelByAdmin_assignedReleasesDispatchNotifiesAndRefundsLast() {
        Payment payment = paidPayment(ShipmentStatus.ASSIGNED, "toss-key");
        Shipment shipment = payment.getShipment();
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));
        when(dispatchService.getActiveDriver(1L)).thenReturn(30L);
        when(memberService.findById(1L)).thenReturn(Optional.of(admin));
        when(memberService.findById(30L)).thenReturn(Optional.of(driverMember));

        paymentService.cancelByAdmin(1L, "고객 요청", 1L);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.CANCELED);
        assertThat(shipment.getStatus()).isEqualTo(ShipmentStatus.CANCELED);
        InOrder order = inOrder(statusHistoryService, dispatchApplicationService, dispatchService, entityManager,
                notificationService, tossPaymentClient);
        order.verify(statusHistoryService).record(shipment, ShipmentStatus.ASSIGNED, ShipmentStatus.CANCELED,
                admin, "관리자 취소: 고객 요청");
        order.verify(dispatchApplicationService).closePendingApplications(shipment);
        order.verify(dispatchService).releaseDispatch(1L);
        order.verify(entityManager).flush();
        order.verify(notificationService).send(eq(shipment.getCustomer()), eq(NotificationType.STATUS_CHANGED),
                anyString(), eq(shipment));
        order.verify(notificationService).send(eq(driverMember), eq(NotificationType.STATUS_CHANGED),
                anyString(), eq(shipment));
        order.verify(tossPaymentClient).cancelPayment("toss-key", "고객 요청");
    }

    @Test
    @DisplayName("접수(배차 전) 결제 건: 배차는 건드리지 않고 고객에게만 알리고 환불한다")
    void cancelByAdmin_requestedRefundsWithoutDispatch() {
        Payment payment = paidPayment(ShipmentStatus.REQUESTED, "toss-key");
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));
        when(memberService.findById(1L)).thenReturn(Optional.of(admin));

        paymentService.cancelByAdmin(1L, "중복 접수", 1L);

        assertThat(payment.getShipment().getStatus()).isEqualTo(ShipmentStatus.CANCELED);
        verifyNoInteractions(dispatchService);
        verify(notificationService).send(eq(payment.getShipment().getCustomer()), eq(NotificationType.STATUS_CHANGED),
                anyString(), eq(payment.getShipment()));
        verify(tossPaymentClient).cancelPayment("toss-key", "중복 접수");
    }

    @ParameterizedTest
    @EnumSource(value = ShipmentStatus.class, names = {"PICKED_UP", "IN_TRANSIT", "ARRIVED", "DELIVERED", "COMPLETED", "CANCELED"})
    @DisplayName("상차 뒤 · 이미 취소된 배송은 관리자도 취소할 수 없고 아무것도 바뀌지 않는다 (409 CANNOT_CANCEL)")
    void cancelByAdmin_notAfterPickup(ShipmentStatus status) {
        Payment payment = paidPayment(status, "toss-key");
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));

        assertThatThrownBy(() -> paymentService.cancelByAdmin(1L, "고객 요청", 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.CANNOT_CANCEL);
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PAID);
        verifyNoInteractions(tossPaymentClient, statusHistoryService, dispatchApplicationService,
                dispatchService, notificationService, memberService);
    }

    @Test
    @DisplayName("개발용 결제 건너뛰기 건은 토스를 부르지 않는다 (배차 해제는 한다)")
    void cancelByAdmin_localSkipWithoutToss() {
        Payment payment = paidPayment(ShipmentStatus.ASSIGNED, "LOCAL_SKIP_order-1");
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));
        when(dispatchService.getActiveDriver(1L)).thenReturn(null);  // 배정 기사를 못 찾아도 취소는 된다
        when(memberService.findById(1L)).thenReturn(Optional.of(admin));

        paymentService.cancelByAdmin(1L, "점검", 1L);

        verify(dispatchService).releaseDispatch(1L);
        verify(memberService, never()).findById(30L);
        verifyNoInteractions(tossPaymentClient);
    }

    @Test
    @DisplayName("토스 환불이 실패하면 예외가 그대로 나가 롤백된다 (DB · 배차 해제는 환불 전에 flush)")
    void cancelByAdmin_refundFailurePropagates() {
        Payment payment = paidPayment(ShipmentStatus.ASSIGNED, "toss-key");
        when(paymentRepository.findByShipmentIdForUpdate(1L)).thenReturn(Optional.of(payment));
        when(dispatchService.getActiveDriver(1L)).thenReturn(30L);
        when(memberService.findById(1L)).thenReturn(Optional.of(admin));
        when(memberService.findById(30L)).thenReturn(Optional.of(driverMember));
        doThrow(new CustomException(ErrorCode.PAYMENT_FAILED, "토스 결제 취소에 실패했습니다."))
                .when(tossPaymentClient).cancelPayment("toss-key", "고객 요청");

        assertThatThrownBy(() -> paymentService.cancelByAdmin(1L, "고객 요청", 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PAYMENT_FAILED);
        verify(dispatchService).releaseDispatch(1L);
        verify(entityManager).flush();
    }

    // 결제 완료 건 (고객 10번)
    private Payment paidPayment(ShipmentStatus shipmentStatus, String paymentKey) {
        Member customer = Member.builder()
                .id(10L)
                .role(MemberRole.CUSTOMER)
                .build();
        Shipment shipment = Shipment.builder()
                .customer(customer)
                .fare(10000)
                .status(shipmentStatus)
                .pickupAt(LocalDateTime.now().plusDays(1))
                .build();
        return Payment.builder()
                .shipment(shipment)
                .orderId("order-1")
                .paymentKey(paymentKey)
                .amount(10000)
                .status(PaymentStatus.PAID)
                .build();
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
