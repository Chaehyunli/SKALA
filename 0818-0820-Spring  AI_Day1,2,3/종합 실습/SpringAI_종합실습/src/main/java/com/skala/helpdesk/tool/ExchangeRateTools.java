package com.skala.helpdesk.tool;

import com.skala.helpdesk.market.ExchangeRate;
import com.skala.helpdesk.market.PriceService;
import com.skala.helpdesk.service.ToolCallRecorder;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/** 매수/매도와 무관하게 환율만 물어볼 때 쓰는 도구. */
@Component
public class ExchangeRateTools {

    private final PriceService priceService;
    private final ToolCallRecorder recorder;

    public ExchangeRateTools(PriceService priceService, ToolCallRecorder recorder) {
        this.priceService = priceService;
        this.recorder = recorder;
    }

    @Tool(description = """
            두 통화 간 환율을 조회한다. base 통화 1단위가 quote 통화로 얼마인지 반환한다.
            '환율 얼마야', '달러가 지금 몇 원이야' 같은 질문에 쓴다. 특별한 언급이 없으면 base=USD, quote=KRW.
            """)
    public ExchangeRate getRate(@ToolParam(description = "기준 통화 코드. 예: USD") String base,
                                 @ToolParam(description = "대상 통화 코드. 예: KRW") String quote) {
        return recorder.execute("getRate", () -> priceService.getRate(base, quote));
    }
}
