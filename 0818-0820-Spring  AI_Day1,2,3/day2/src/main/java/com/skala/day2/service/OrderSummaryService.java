package com.skala.day2.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import com.skala.day2.domain.Order;
import com.skala.day2.repository.OrderRepository;
import com.skala.day2.web.dto.lab1.response.SummaryResponse;

/**
 * 업무 흐름은 여기. 주문을 못 찾으면 모델을 부르지 않는다.
 *
 * <p>서비스는 무엇을 하는가만 읽혀야 한다.
 */
@Service
public class OrderSummaryService {

    private final OrderRepository orders;          // 1장에서 만든 계층을 그대로
    private final ChatClient summaryChat;

    public OrderSummaryService(OrderRepository orders, ChatClient lab1SummaryChatClient) {
        this.orders = orders;
        this.summaryChat = lab1SummaryChatClient;
    }

    public SummaryResponse summarize(String orderId, String userId) {
        Order order = orders.findByIdAndOwnerId(orderId, userId)   // 권한은 쿼리 안에
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        String summary = summaryChat.prompt()
                .user(u -> u.text("주문번호 {id} · 상품 {item} · 상태 {status} · 도착예정 {eta}"
                                + "\n위 정보를 한 문장으로 요약해 줘.")
                        .param("id", order.id()).param("item", order.item())
                        .param("status", order.status().label()).param("eta", order.eta()))
                .call().content();
        return new SummaryResponse(order.id(), summary);
    }
}
