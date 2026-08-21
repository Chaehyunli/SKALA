package com.skala.helpdesk.advisor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * 토큰·지연 실측 — /actuator/metrics/ai.tokens, ai.latency 로 확인한다.
 * 체인 가장 안쪽(order 900)에 둬서 실제 모델 호출 구간만 잰다. (day3 TokenMeterAdvisor 포팅)
 *
 * <p>P95는 {@code management.metrics.distribution.percentiles.ai.latency} 설정(이름 기반 필터)에
 * 기대지 않고 여기서 직접 {@code publishPercentiles}로 켠다 — 이름 매칭에 기대면 조용히 안 먹었을 때
 * 알아챌 방법이 없다. 윈도우(30분)는 짧게 잡지 않는다 — 짧으면 캡처·화면 전환하는 동안 최근 값이
 * 빠져나가 MAX·P95가 도로 0으로 보인다(실습 세션 하나 도는 동안은 값이 안 꺼져야 한다).
 */
public class TokenMeterAdvisor implements CallAdvisor, StreamAdvisor {

    private static final String FEATURE = "helpdesk-chat";

    private final MeterRegistry registry;
    private final Timer latencyTimer;

    public TokenMeterAdvisor(MeterRegistry registry) {
        this.registry = registry;
        this.latencyTimer = Timer.builder("ai.latency")
                .tag("phase", "model")
                .publishPercentiles(0.95)
                .distributionStatisticExpiry(Duration.ofMinutes(30))
                .register(registry);
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
        latencyTimer.record(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS);
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
