package com.skala.day2.service;

/** 주문이 없거나 남의 주문일 때. 존재 여부를 알리지 않기 위해 두 경우를 구분하지 않는다. */
public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException(String orderId) {
        super("주문을 찾을 수 없습니다: " + orderId);
    }
}
