package com.skala.helpdesk.tool;

import com.skala.helpdesk.market.PriceService;
import com.skala.helpdesk.market.StockQuote;
import com.skala.helpdesk.service.ToolCallRecorder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 매수/매도와 무관하게 시세만 물어볼 때 쓰는 도구. */
@Component
public class StockQuoteTools {

    private final PriceService priceService;
    private final ToolCallRecorder recorder;

    public StockQuoteTools(PriceService priceService, ToolCallRecorder recorder) {
        this.priceService = priceService;
        this.recorder = recorder;
    }

    @Tool(description = """
            해외 주식 종목의 현재가(달러)를 조회한다.
            '지금 얼마야', '시세 알려줘' 처럼 매수/매도 없이 가격만 물으면 이 도구를 쓴다.
            """)
    public StockQuote getQuote(@ToolParam(description = "종목 코드. 예: AAPL, TSLA") String symbol) {
        return recorder.execute("getQuote", () -> priceService.getQuote(symbol));
    }
}
