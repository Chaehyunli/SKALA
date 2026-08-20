package com.skala.day3.domain;

/** 환불 접수 티켓. 모델은 PENDING 까지만 만들 수 있고, APPROVED 는 사람만 만든다. */
public record Ticket(String no, String orderId, String userId, String reason, TicketStatus status) {

    public Ticket approve() {
        return new Ticket(no, orderId, userId, reason, TicketStatus.APPROVED);
    }
}
