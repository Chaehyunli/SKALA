package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.kafka.PaymentKafkaProducer;
import com.lecture.payment.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentKafkaProducer kafkaProducer;

    @Mock
    private UserServiceClient userServiceClient;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void coursePaymentPublishesPaymentAmountWithoutCreditData() {
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentDto.InternalPaymentRequest request = PaymentDto.InternalPaymentRequest.builder()
                .userId(7L)
                .courseId(12L)
                .amount(new BigDecimal("5500.00"))
                .build();

        PaymentDto.InternalPaymentResult response = paymentService.processInternalPayment(request);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getCourseId()).isEqualTo(12L);
        assertThat(payment.getValue().getAmount()).isEqualByComparingTo("5500.00");
        assertThat(payment.getValue().getCreditAmount()).isNull();

        ArgumentCaptor<PaymentKafkaProducer.PaymentCompletedEvent> event =
                ArgumentCaptor.forClass(PaymentKafkaProducer.PaymentCompletedEvent.class);
        verify(kafkaProducer).publishPaymentCompleted(event.capture());
        assertThat(event.getValue().getCourseId()).isEqualTo(12L);
        assertThat(event.getValue().getAmount()).isEqualByComparingTo("5500.00");
        assertThat(event.getValue().getCreditAmount()).isNull();
        assertThat(event.getValue().getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void creditChargePublishesPurchasedCreditsSeparatelyFromPaymentAmount() {
        when(userServiceClient.getCredit(7L)).thenReturn(new BigDecimal("100.00"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));
        PaymentDto.CreditChargeRequest request = PaymentDto.CreditChargeRequest.builder()
                .credits(5_000L)
                .amount(new BigDecimal("5500.00"))
                .build();

        PaymentDto.CreditChargeResponse response = paymentService.chargeCredits(7L, request);

        ArgumentCaptor<Payment> payment = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(payment.capture());
        assertThat(payment.getValue().getCourseId()).isNull();
        assertThat(payment.getValue().getAmount()).isEqualByComparingTo("5500.00");
        assertThat(payment.getValue().getCreditAmount()).isEqualByComparingTo("5000.00");
        assertThat(payment.getValue().getStatus()).isEqualTo(Payment.Status.COMPLETED);

        ArgumentCaptor<PaymentKafkaProducer.PaymentCompletedEvent> event =
                ArgumentCaptor.forClass(PaymentKafkaProducer.PaymentCompletedEvent.class);
        verify(kafkaProducer).publishPaymentCompleted(event.capture());
        assertThat(event.getValue().getAmount()).isEqualByComparingTo("5500.00");
        assertThat(event.getValue().getCreditAmount()).isEqualByComparingTo("5000.00");
        assertThat(response.getCredited()).isEqualByComparingTo("5000.00");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
    }
}
