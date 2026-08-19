package com.skala.day2.domain;

/** 주문 상태 — 화면·요약에 쓸 한국어 표시는 여기서만 정한다. */
public enum OrderStatus {

    PENDING("결제 대기"),
    PREPARING("상품 준비중"),
    SHIPPING("배송 중"),
    DELIVERED("배송 완료");

    private final String label;

    OrderStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
