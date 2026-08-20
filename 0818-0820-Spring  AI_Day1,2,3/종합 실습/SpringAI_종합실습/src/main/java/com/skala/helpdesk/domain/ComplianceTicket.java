package com.skala.helpdesk.domain;

/** 해외주식 보유 신고 티켓 — 매수 직후 평가액이 신고 기준액을 넘으면 자동 생성된다. */
public record ComplianceTicket(String no, String userId, String symbol, double valuationUsd, TicketStatus status) {

    public ComplianceTicket approve() {
        return new ComplianceTicket(no, userId, symbol, valuationUsd, TicketStatus.APPROVED);
    }
}
