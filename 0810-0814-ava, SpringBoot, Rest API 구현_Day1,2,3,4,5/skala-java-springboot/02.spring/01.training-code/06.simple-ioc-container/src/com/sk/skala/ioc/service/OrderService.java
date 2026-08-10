package com.sk.skala.ioc.service;

import com.sk.skala.ioc.annotation.Service;

/**
 * OrderService - UserService 에 의존하는 최상위 서비스.
 * IoC 컨테이너가 UserService(그리고 그 아래 DatabaseService)를 재귀적으로 해결해서 주입한다.
 */
@Service
public class OrderService {

    private final UserService userService; // 생성자 주입 필드

    // 생성자 주입 - IoC 컨테이너가 이 생성자를 통해 UserService 를 자동 주입
    public OrderService(UserService userService) {
        this.userService = userService;
        System.out.println("  OrderService 생성자 호출 (UserService 주입됨)");
    }

    public String processOrder(String userName) {
        String userInfo = userService.getUser(userName);
        return "[ORDER] 주문 처리 완료 - " + userInfo;
    }

    @Override
    public String toString() {
        return "OrderService@" + Integer.toHexString(hashCode())
                + " -> (userService=" + userService + ")";
    }
}
