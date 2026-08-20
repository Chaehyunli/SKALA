package com.skala.helpdesk.service;

/** 존재하지 않는 신고 티켓 번호로 승인을 시도했을 때. */
public class ComplianceTicketNotFoundException extends RuntimeException {

    public ComplianceTicketNotFoundException(String no) {
        super("신고 티켓을 찾을 수 없습니다: " + no);
    }
}
