package com.skala.helpdesk.domain;

/** 신고 티켓 상태 — PENDING 은 모델(매수 도구)이 만들 수 있고, APPROVED 는 사람만 만들 수 있다. */
public enum TicketStatus {
    PENDING,
    APPROVED
}
