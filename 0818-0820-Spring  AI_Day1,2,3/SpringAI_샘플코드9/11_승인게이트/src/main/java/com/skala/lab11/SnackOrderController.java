package com.skala.lab11;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 11장 미니 실습 — 접수는 도구가, 승인은 사람이.
 *
 * <p>{@code /lab11/approve} 는 도구 목록에 없다. 그래서 모델은 부를 수 없다.
 */
@RestController
@Tag(name = "11장 실습 · 승인 게이트")
public class SnackOrderController {

    private final ChatClient chat;
    private final SnackTools tools;
    private final Tickets tickets;

    public SnackOrderController(ChatClient.Builder builder, SnackTools tools, Tickets tickets) {
        this.chat = builder.build();
        this.tools = tools;
        this.tickets = tickets;
    }

    @GetMapping("/lab11/ask")
    @Operation(summary = "주문을 말로 시킨다", description = "즉시 처리되지 않고 접수만 되어야 정상이다.")
    public String ask(@RequestParam String q,
                      @RequestParam(defaultValue = "user1") String userId) {
        return chat.prompt()
                .user(q)
                .tools(tools)
                .toolContext(Map.of("userId", userId))   // ID 는 컨텍스트로 — 모델이 못 바꾼다
                .call().content();
    }

    @GetMapping("/lab11/tickets/pending")
    @Operation(summary = "승인 대기 목록")
    public List<Tickets.Ticket> pending() {
        return tickets.pending();
    }

    @PostMapping("/lab11/approve")
    @Operation(summary = "사람이 누르는 승인", description = "도구 목록에 없으므로 모델은 이 경로에 닿을 수 없다.")
    public Map<String, Object> approve(@RequestParam String no) {
        return tickets.approve(no)
                .<Map<String, Object>>map(t -> Map.of("no", t.no(), "status", t.status().name()))
                .orElse(Map.of("no", no, "status", "NOT_FOUND"));
    }
}
