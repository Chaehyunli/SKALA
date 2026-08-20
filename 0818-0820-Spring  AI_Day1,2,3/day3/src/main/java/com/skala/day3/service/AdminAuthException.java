package com.skala.day3.service;

/** X-Admin-Key 헤더가 없거나 설정값과 다를 때. */
public class AdminAuthException extends RuntimeException {

    public AdminAuthException() {
        super("관리자 인증에 실패했습니다.");
    }
}
