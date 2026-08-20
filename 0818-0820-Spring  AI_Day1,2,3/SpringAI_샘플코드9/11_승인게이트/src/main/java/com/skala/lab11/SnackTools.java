package com.skala.lab11;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 11장 미니 실습 — 간식 주문 승인 게이트.
 *
 * <p>주문이 즉시 처리되면 실패다. 접수만 되어야 정상이다.
 * 사용자 ID 는 모델이 준 값이 아니라 {@link ToolContext} 에서 꺼낸다.
 */
@Component
public class SnackTools {

    private final Tickets tickets;

    public SnackTools(Tickets tickets) {
        this.tickets = tickets;
    }

    @Tool(description = "간식을 주문한다. 즉시 결제되지 않고 팀장 승인 후 처리된다.")
    public String 간식주문(@ToolParam(description = "품목과 수량") String 품목,
                        ToolContext ctx) {
        String 사용자 = (String) ctx.getContext().get("userId");   // ID 는 모델이 아니라 여기서
        var 티켓 = tickets.create(사용자, 품목);                    // 접수만 — 상태 PENDING
        return "%s 주문 접수(%s). 팀장 승인 후 결제됩니다.".formatted(품목, 티켓.no());
    }
}
