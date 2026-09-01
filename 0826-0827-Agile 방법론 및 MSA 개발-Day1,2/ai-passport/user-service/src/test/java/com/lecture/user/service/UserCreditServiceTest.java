package com.lecture.user.service;

import com.lecture.user.entity.User;
import com.lecture.user.entity.UserCreditTransaction;
import com.lecture.user.kafka.PaymentCompletedEvent;
import com.lecture.user.repository.UserCreditTransactionRepository;
import com.lecture.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserCreditServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserCreditTransactionRepository transactionRepository;

    @InjectMocks
    private UserCreditService userCreditService;

    @Test
    void completedCreditPaymentAddsCreditAmountToUserCredit() {
        User user = User.builder()
                .id(7L)
                .email("student@example.com")
                .password("encoded-password")
                .name("Student")
                .role(User.Role.STUDENT)
                .credit(new BigDecimal("100.00"))
                .build();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                31L, 7L, null, new BigDecimal("5500.00"), new BigDecimal("5000.00"), "COMPLETED");

        when(transactionRepository.existsById(31L)).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userCreditService.creditPayment(event);

        assertThat(user.getCredit()).isEqualByComparingTo("5100.00");
        ArgumentCaptor<UserCreditTransaction> transaction = ArgumentCaptor.forClass(UserCreditTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getPaymentId()).isEqualTo(31L);
        assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void legacyCreditPaymentUsesAmountWhenCreditAmountIsMissing() {
        User user = User.builder()
                .id(7L)
                .email("student@example.com")
                .password("encoded-password")
                .name("Student")
                .role(User.Role.STUDENT)
                .credit(new BigDecimal("100.00"))
                .build();
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                31L, 7L, null, new BigDecimal("5000.00"), null, "COMPLETED");

        when(transactionRepository.existsById(31L)).thenReturn(false);
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        userCreditService.creditPayment(event);

        assertThat(user.getCredit()).isEqualByComparingTo("5100.00");
        ArgumentCaptor<UserCreditTransaction> transaction = ArgumentCaptor.forClass(UserCreditTransaction.class);
        verify(transactionRepository).save(transaction.capture());
        assertThat(transaction.getValue().getPaymentId()).isEqualTo(31L);
        assertThat(transaction.getValue().getAmount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void coursePaymentDoesNotAddCredit() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                31L, 7L, 12L, new BigDecimal("5500.00"), new BigDecimal("5500.00"), "COMPLETED");

        userCreditService.creditPayment(event);

        verifyNoInteractions(userRepository, transactionRepository);
    }

    @Test
    void duplicatedPaymentEventDoesNotAddCreditAgain() {
        PaymentCompletedEvent event = new PaymentCompletedEvent(
                31L, 7L, null, new BigDecimal("5500.00"), new BigDecimal("5000.00"), "COMPLETED");
        when(transactionRepository.existsById(31L)).thenReturn(true);

        userCreditService.creditPayment(event);

        verify(userRepository, never()).findById(7L);
        verify(transactionRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void analysisCreditIsDeductedWhenBalanceIsSufficient() {
        User user = User.builder()
                .id(7L)
                .email("student@example.com")
                .password("encoded-password")
                .name("Student")
                .role(User.Role.STUDENT)
                .credit(new BigDecimal("240.00"))
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        BigDecimal remaining = userCreditService.deductForAnalysis(7L, new BigDecimal("120.00"));

        assertThat(remaining).isEqualByComparingTo("120.00");
        assertThat(user.getCredit()).isEqualByComparingTo("120.00");
    }

    @Test
    void analysisCreditIsRejectedWhenBalanceIsInsufficient() {
        User user = User.builder()
                .id(7L)
                .email("student@example.com")
                .password("encoded-password")
                .name("Student")
                .role(User.Role.STUDENT)
                .credit(new BigDecimal("119.00"))
                .build();
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> userCreditService.deductForAnalysis(7L, new BigDecimal("120.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("크레딧이 부족합니다");
        assertThat(user.getCredit()).isEqualByComparingTo("119.00");
    }
}
