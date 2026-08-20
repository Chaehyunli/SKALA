package com.skala.day3.advisor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * 턴 단위 감사 로그 — 누가 무엇을 물었고 얼마나 걸렸는지 한 줄로 남긴다.
 * 체인 가장 바깥(order 0)에 둬서 뒤에서 무엇이 막히든 항상 기록된다.
 *
 * <p>턴이 진행되는 동안 userId·traceId 를 MDC 에 심어 둔다 — 같은 스레드에서 실행되는
 * {@link com.skala.day3.observation.ToolCallLoggingHandler} 도 이 값을 그대로 찍을 수 있다.
 */
public class AuditAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String MDC_USER_ID = "userId";
    public static final String MDC_TRACE_ID = "traceId";

    private static final Logger log = LoggerFactory.getLogger(AuditAdvisor.class);

    @Override
    public String getName() {
        return "AuditAdvisor";
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        String traceId = traceId();
        String userId = userId(request);
        MDC.put(MDC_TRACE_ID, traceId);
        MDC.put(MDC_USER_ID, userId);
        try {
            log.info("[{}] {} 질문=\"{}\"", traceId, userId, question(request));
            long start = System.nanoTime();
            ChatClientResponse response = chain.nextCall(request);
            log.info("[{}] {} 응답 {}ms", traceId, userId, elapsedMs(start));
            return response;
        } finally {
            MDC.remove(MDC_TRACE_ID);
            MDC.remove(MDC_USER_ID);
        }
    }

    // MDC 는 스트리밍(Reactor 스케줄러 전환)에서는 스레드가 바뀌어 안정적으로 전파되지 않는다.
    // 지금 Lab3 는 스트리밍 엔드포인트가 없어 이 경로는 실사용되지 않으므로 턴 로그만 남긴다.
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        String traceId = traceId();
        String userId = userId(request);
        log.info("[{}] {} 질문=\"{}\"", traceId, userId, question(request));
        long start = System.nanoTime();
        return chain.nextStream(request)
                .doOnComplete(() -> log.info("[{}] {} 응답 {}ms", traceId, userId, elapsedMs(start)));
    }

    private String traceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String userId(ChatClientRequest request) {
        Object userId = request.context().get("userId");
        return userId != null ? userId.toString() : "-";
    }

    private String question(ChatClientRequest request) {
        return request.prompt().getUserMessage().getText();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
