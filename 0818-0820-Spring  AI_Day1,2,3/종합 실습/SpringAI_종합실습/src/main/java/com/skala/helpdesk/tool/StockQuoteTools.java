package com.skala.helpdesk.tool;

import com.skala.helpdesk.market.PriceService;
import com.skala.helpdesk.market.StockQuote;
import com.skala.helpdesk.service.ToolUsageTracker;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 매수/매도와 무관하게 시세만 물어볼 때 쓰는 도구. */
@Component
public class StockQuoteTools {

    private static final Logger log = LoggerFactory.getLogger(StockQuoteTools.class);

    private final PriceService priceService;
    private final MeterRegistry meterRegistry;
    private final ToolUsageTracker usageTracker;

    public StockQuoteTools(PriceService priceService, MeterRegistry meterRegistry, ToolUsageTracker usageTracker) {
        this.priceService = priceService;
        this.meterRegistry = meterRegistry;
        this.usageTracker = usageTracker;
    }

    @Tool(description = """
            해외 주식 종목의 현재가(달러)를 조회한다.
            '지금 얼마야', '시세 알려줘' 처럼 매수/매도 없이 가격만 물으면 이 도구를 쓴다.
            """)
    public StockQuote getQuote(@ToolParam(description = "종목 코드. 예: AAPL, TSLA") String symbol) {
        boolean ok = false;
        try {
            usageTracker.markUsed();
            StockQuote quote = priceService.getQuote(symbol);
            ok = true;
            return quote;
        } finally {
            String result = ok ? "ok" : "fail";
            meterRegistry.counter("ai.tool.calls", "tool", "getQuote", "result", result).increment();
            log.info("tool=getQuote result={}", result);
        }
    }
}
