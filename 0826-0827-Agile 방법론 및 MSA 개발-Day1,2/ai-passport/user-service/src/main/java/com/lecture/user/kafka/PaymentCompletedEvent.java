package com.lecture.user.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PaymentCompletedEvent {
    private Long paymentId;
    private Long userId;
    private Long courseId;
    private BigDecimal amount;
    private BigDecimal creditAmount;
    private String status;
}
