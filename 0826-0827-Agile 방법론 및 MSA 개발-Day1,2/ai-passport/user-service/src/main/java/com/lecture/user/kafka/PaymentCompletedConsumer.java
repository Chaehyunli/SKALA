package com.lecture.user.kafka;

import com.lecture.user.service.UserCreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentCompletedConsumer {

    private final UserCreditService userCreditService;

    @KafkaListener(
            topics = "${kafka.topic.payment-completed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(PaymentCompletedEvent event) {
        if (event == null || (event.getCreditAmount() == null && event.getAmount() == null)) {
            log.warn("[Kafka Consumer] 적립 금액이 없는 기존 payment.completed 이벤트를 건너뜁니다 - paymentId: {}",
                    event == null ? null : event.getPaymentId());
            return;
        }
        log.info("[Kafka Consumer] payment.completed 수신 - paymentId: {}, userId: {}, creditAmount: {}",
                event.getPaymentId(), event.getUserId(),
                event.getCreditAmount() != null ? event.getCreditAmount() : event.getAmount());
        userCreditService.creditPayment(event);
    }
}
