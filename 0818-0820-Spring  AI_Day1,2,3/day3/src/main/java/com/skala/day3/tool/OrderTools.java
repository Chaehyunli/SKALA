package com.skala.day3.tool;

import com.skala.day3.repository.OrderRepository;
import com.skala.day3.repository.TicketRepository;
import com.skala.day3.web.dto.lab3.OrderView;
import com.skala.day3.web.dto.lab3.TicketView;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 모델에 노출되는 도구. 사용자 식별은 파라미터가 아니라 ToolContext 로만 받는다 —
 * 모델이 프롬프트로 다른 사용자의 userId 를 흉내 내도 여기엔 반영되지 않는다.
 */
@Component
public class OrderTools {

    private final OrderRepository orders;
    private final TicketRepository tickets;
    private final MeterRegistry meterRegistry;

    public OrderTools(OrderRepository orders, TicketRepository tickets, MeterRegistry meterRegistry) {
        this.orders = orders;
        this.tickets = tickets;
        this.meterRegistry = meterRegistry;
    }

    @Tool(description = """
            주문 상태를 조회한다. 사용자가 주문번호를 말하거나
            '내 주문', '배송 언제' 처럼 물으면 이 도구를 쓴다.
            """)
    public OrderView getOrder(@ToolParam(description = "조회할 주문번호. 예: 12345") String orderId,
                               ToolContext context) {
        boolean ok = false;
        try {
            OrderView view = orders.findByIdAndOwnerId(orderId, userId(context))
                    .map(OrderView::from)
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
            ok = true;
            return view;
        } finally {
            recordCall("getOrder", ok);
        }
    }

    @Tool(description = "환불을 접수한다. 즉시 처리되지 않고 담당자 승인 후 처리된다.")
    public TicketView requestRefund(@ToolParam(description = "환불할 주문번호") String orderId,
                                     @ToolParam(description = "환불 사유") String reason,
                                     ToolContext context) {
        boolean ok = false;
        try {
            String userId = userId(context);
            orders.findByIdAndOwnerId(orderId, userId)                 // 권한 먼저 — 남의 주문은 접수도 안 된다
                    .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다."));
            TicketView view = TicketView.from(tickets.create(orderId, userId, reason));
            ok = true;
            return view;
        } finally {
            recordCall("requestRefund", ok);
        }
    }

    private String userId(ToolContext context) {
        return (String) context.getContext().get("userId");
    }

    private void recordCall(String tool, boolean ok) {
        meterRegistry.counter("ai.tool.calls", "tool", tool, "result", ok ? "ok" : "fail").increment();
    }
}
