package com.skala.day3.web.dto.lab3;

import com.skala.day3.domain.Order;
import io.swagger.v3.oas.annotations.media.Schema;

/** getOrder 도구가 모델에 돌려주는 모양 — Order 도메인의 ownerId 는 감춘다. */
public record OrderView(
        @Schema(example = "12345") String orderId,
        @Schema(example = "무선 이어폰") String item,
        @Schema(description = "PENDING · PREPARING · SHIPPING · DELIVERED 의 한글 표시", example = "배송 중") String status,
        @Schema(example = "7월 30일") String eta) {

    public static OrderView from(Order order) {
        return new OrderView(order.id(), order.item(), order.status().label(), order.eta());
    }
}
