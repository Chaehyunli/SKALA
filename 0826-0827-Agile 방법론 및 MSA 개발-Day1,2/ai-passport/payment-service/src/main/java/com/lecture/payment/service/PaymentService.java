package com.lecture.payment.service;

import com.lecture.payment.dto.PaymentDto;
import com.lecture.payment.entity.Payment;
import com.lecture.payment.kafka.PaymentKafkaProducer;
import com.lecture.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentKafkaProducer kafkaProducer;
    private final UserServiceClient userServiceClient;

    /**
     * 내부 결제 요청 (Enrollment Service → Payment Service REST 호출)
     * 실습 환경에서는 PG 연동 없이 항상 성공으로 처리
     *
     * 처리 흐름:
     * 1. Payment 생성 (PENDING)
     * 2. PG 결제 처리 (실습: UUID 트랜잭션 ID 발급으로 대체)
     * 3. Payment 상태 → COMPLETED
     * 4. payment.completed 이벤트 발행 → Kafka
     */
    @Transactional
    public PaymentDto.InternalPaymentResult processInternalPayment(
            PaymentDto.InternalPaymentRequest request) {

        log.info("[PaymentService] 결제 요청 - userId: {}, courseId: {}, amount: {}",
                request.getUserId(), request.getCourseId(), request.getAmount());

        Payment payment = paymentRepository.save(
                Payment.builder()
                        .userId(request.getUserId())
                        .courseId(request.getCourseId())
                        .amount(request.getAmount())
                        .build()
        );

        try {
            String transactionId = UUID.randomUUID().toString();

            payment.complete(transactionId);
            log.info("[PaymentService] 결제 완료 처리 - paymentId: {}, transactionId: {}",
                    payment.getId(), transactionId);

            kafkaProducer.publishPaymentCompleted(
                    PaymentKafkaProducer.PaymentCompletedEvent.builder()
                            .paymentId(payment.getId())
                            .userId(request.getUserId())
                            .courseId(request.getCourseId())
                            .amount(payment.getAmount())
                            .status("COMPLETED")
                            .build()
            );

            log.info("[PaymentService] 결제 최종 성공 - paymentId: {}", payment.getId());

            return PaymentDto.InternalPaymentResult.builder()
                    .paymentId(payment.getId())
                    .status("COMPLETED")
                    .build();

        } catch (Exception e) {
            payment.fail();

            log.error("[PaymentService] 결제 실패 - paymentId: {}, userId: {}, courseId: {}, error: {}",
                    payment.getId(),
                    request.getUserId(),
                    request.getCourseId(),
                    e.getMessage(),
                    e);

            return PaymentDto.InternalPaymentResult.builder()
                    .paymentId(payment.getId())
                    .status("FAILED")
                    .build();
        }
    }

    /**
     * 웹 클라이언트의 크레딧 패키지 결제.
     * 결제 금액(amount)과 실제 적립량(credits)을 구분해 저장하고 이벤트로 전달한다.
     */
    @Transactional
    public PaymentDto.CreditChargeResponse chargeCredits(
            Long userId, PaymentDto.CreditChargeRequest request) {

        // 결제를 생성하기 전에 사용자 존재 여부와 현재 크레딧 조회 가능 여부를 검증한다.
        userServiceClient.getCredit(userId);
        BigDecimal creditAmount = BigDecimal.valueOf(request.getCredits());
        Payment payment = paymentRepository.save(Payment.builder()
                .userId(userId)
                .amount(request.getAmount())
                .creditAmount(creditAmount)
                .build());

        try {
            payment.complete(UUID.randomUUID().toString());
            kafkaProducer.publishPaymentCompleted(
                    PaymentKafkaProducer.PaymentCompletedEvent.builder()
                            .paymentId(payment.getId())
                            .userId(userId)
                            .amount(payment.getAmount())
                            .creditAmount(payment.getCreditAmount())
                            .status("COMPLETED")
                            .build());

            log.info("[PaymentService] 크레딧 충전 결제 완료 - paymentId: {}, userId: {}, amount: {}, credits: {}",
                    payment.getId(), userId, payment.getAmount(), payment.getCreditAmount());
            return PaymentDto.CreditChargeResponse.builder()
                    .paymentId(payment.getId())
                    .credited(payment.getCreditAmount())
                    .status("COMPLETED")
                    .build();
        } catch (Exception e) {
            payment.fail();
            log.error("[PaymentService] 크레딧 충전 결제 실패 - paymentId: {}, userId: {}",
                    payment.getId(), userId, e);
            throw new IllegalStateException("크레딧 충전 결제에 실패했습니다", e);
        }
    }

    public PaymentDto.BalanceResponse getBalance(Long userId) {
        return PaymentDto.BalanceResponse.builder()
                .balance(userServiceClient.getCredit(userId))
                .creditPerAnalysis(120)
                .build();
    }

    /**
     * 결제 단건 조회
     */
    public PaymentDto.PaymentResponse getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("결제 정보를 찾을 수 없습니다: " + id));
        return PaymentDto.PaymentResponse.from(payment);
    }

    /**
     * 사용자 결제 내역 조회
     */
    public List<PaymentDto.PaymentResponse> getPaymentsByUser(Long userId) {
        return paymentRepository.findByUserId(userId).stream()
                .map(PaymentDto.PaymentResponse::from)
                .collect(Collectors.toList());
    }
}
