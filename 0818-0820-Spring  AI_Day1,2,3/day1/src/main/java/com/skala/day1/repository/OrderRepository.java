package com.skala.day1.repository;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.skala.day1.domain.Order;
import com.skala.day1.domain.OrderStatus;

/**
 * 데이터에 닿는 유일한 자리. 실제 DB 는 쓰지 않는다 — 나중에 JPA 로 바꿔도 위 계층은 그대로다.
 *
 * <p>{@code 99999} 는 user2 소유, {@code 00000} 은 존재하지 않는 주문이다 — 두 실패
 * 케이스({@code findByIdAndOwnerId} 의 소유자 불일치·미존재)를 같은 404 로 다룰 수 있는지
 * 확인하는 데 쓴다.
 */
@Repository
public class OrderRepository {

    private static final Map<String, Order> ORDERS = Map.of(
            "12345", new Order("12345", "user1", "무선 이어폰", OrderStatus.SHIPPING, "7월 30일"),
            "99999", new Order("99999", "user2", "노트북 거치대", OrderStatus.DELIVERED, "7월 20일"));

    public Optional<Order> findByIdAndOwnerId(String orderId, String ownerId) {
        return Optional.ofNullable(ORDERS.get(orderId))
                .filter(order -> order.ownerId().equals(ownerId));
    }
}
