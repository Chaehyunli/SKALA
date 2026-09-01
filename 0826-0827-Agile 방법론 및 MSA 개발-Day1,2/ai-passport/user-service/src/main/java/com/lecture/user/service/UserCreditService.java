package com.lecture.user.service;

import com.lecture.user.entity.User;
import com.lecture.user.entity.UserCreditTransaction;
import com.lecture.user.kafka.PaymentCompletedEvent;
import com.lecture.user.repository.UserCreditTransactionRepository;
import com.lecture.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCreditService {

    private final UserRepository userRepository;
    private final UserCreditTransactionRepository transactionRepository;

    @Transactional
    public void creditPayment(PaymentCompletedEvent event) {
        if (event != null && event.getCourseId() != null) {
            log.info("[UserCreditService] 강의 결제 이벤트는 크레딧 적립 대상이 아닙니다 - paymentId: {}, courseId: {}",
                    event.getPaymentId(), event.getCourseId());
            return;
        }

        validate(event);
        var creditAmount = event.getCreditAmount() != null ? event.getCreditAmount() : event.getAmount();

        if (transactionRepository.existsById(event.getPaymentId())) {
            log.info("[UserCreditService] 이미 처리한 결제 이벤트입니다 - paymentId: {}", event.getPaymentId());
            return;
        }

        User user = userRepository.findById(event.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + event.getUserId()));

        user.addCredit(creditAmount);
        transactionRepository.save(UserCreditTransaction.builder()
                .paymentId(event.getPaymentId())
                .userId(event.getUserId())
                .amount(creditAmount)
                .creditedAt(LocalDateTime.now())
                .build());

        log.info("[UserCreditService] 결제 크레딧 적립 완료 - paymentId: {}, userId: {}, amount: {}, balance: {}",
                event.getPaymentId(), event.getUserId(), creditAmount, user.getCredit());
    }

    @Transactional
    public BigDecimal deductForAnalysis(Long userId, BigDecimal amount) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        user.deductCredit(amount);
        log.info("[UserCreditService] 권한 설계 크레딧 차감 완료 - userId: {}, amount: {}, balance: {}",
                userId, amount, user.getCredit());
        return user.getCredit();
    }

    private void validate(PaymentCompletedEvent event) {
        if (event == null || event.getPaymentId() == null || event.getUserId() == null) {
            throw new IllegalArgumentException("결제 완료 이벤트의 필수 값이 없습니다");
        }
        if (!"COMPLETED".equals(event.getStatus())) {
            throw new IllegalArgumentException("완료되지 않은 결제는 적립할 수 없습니다");
        }
        var creditAmount = event.getCreditAmount() != null ? event.getCreditAmount() : event.getAmount();
        if (creditAmount == null || creditAmount.signum() <= 0) {
            throw new IllegalArgumentException("적립 크레딧은 0보다 커야 합니다");
        }
    }
}
