package com.skala.helpdesk.advisor;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import reactor.core.publisher.Flux;

import java.util.concurrent.TimeUnit;

/**
 * 토큰·지연 실측 — /actuator/metrics/ai.tokens, ai.latency 로 확인한다.
 * 체인 가장 안쪽(order 900)에 둬서 실제 모델 호출 구간만 잰다. (day3 TokenMeterAdvisor 포팅)
 */
public class TokenMeterAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String FEATURE = "helpdesk-chat";

    private final MeterRegistry registry;

    public TokenMeterAdvisor(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public String getName() {
        return "TokenMeterAdvisor";
    }

    @Override
    public int getOrder() {
        return 900;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        long start = System.nanoTime();
        ChatClientResponse response = chain.nextCall(request);
        record(response, start);
        return response;
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        long start = System.nanoTime();
        return chain.nextStream(request)
                .doOnNext(response -> record(response, start));
    }

    private void record(ChatClientResponse response, long startNanos) {
        Usage usage = response.chatResponse() != null ? response.chatResponse().getMetadata().getUsage() : null;
        if (usage == null) {
            return;
        }
        registry.timer("ai.latency", "phase", "model")
                .record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
        if (usage.getPromptTokens() != null) {
            registry.counter("ai.tokens", "type", "prompt", "feature", FEATURE)
                    .increment(usage.getPromptTokens());
        }
        if (usage.getCompletionTokens() != null) {
            registry.counter("ai.tokens", "type", "completion", "feature", FEATURE)
                    .increment(usage.getCompletionTokens());
        }
    }
}
