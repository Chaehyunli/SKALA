package com.skala.day3.service;

/** 존재하지 않는 티켓 번호를 승인하려 할 때. */
public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(String no) {
        super("환불 티켓을 찾을 수 없습니다: " + no);
    }
}
