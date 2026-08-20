package com.skala.helpdesk.service;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * 도구 호출마다 반복되던 "사용 표시 → 실행 → 성공/실패 메트릭·로그" 흐름을 한 곳으로 모은다.
 * StockQuoteTools·ExchangeRateTools·PortfolioTools의 각 @Tool 메서드가 이 클래스를 통해 실행된다.
 */
@Component
public class ToolCallRecorder {

    private static final Logger log = LoggerFactory.getLogger(ToolCallRecorder.class);

    private final MeterRegistry meterRegistry;
    private final ToolUsageTracker usageTracker;

    public ToolCallRecorder(MeterRegistry meterRegistry, ToolUsageTracker usageTracker) {
        this.meterRegistry = meterRegistry;
        this.usageTracker = usageTracker;
    }

    public <T> T execute(String toolName, Supplier<T> action) {
        usageTracker.markUsed();
        boolean ok = false;
        try {
            T result = action.get();
            ok = true;
            return result;
        } finally {
            String result = ok ? "ok" : "fail";
            meterRegistry.counter("ai.tool.calls", "tool", toolName, "result", result).increment();
            log.info("tool={} result={}", toolName, result);
        }
    }
}
