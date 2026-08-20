package com.skala.helpdesk.web.dto.portfolio;

import io.swagger.v3.oas.annotations.media.Schema;

/** 보유 종목 하나 — 평단가와 현재가를 같이 보여줘서 손익을 바로 알 수 있게 한다. */
public record HoldingView(
        @Schema(example = "AAPL") String symbol,
        @Schema(example = "10") int quantity,
        @Schema(description = "평단가(달러)", example = "150.0") double avgBuyPriceUsd,
        @Schema(description = "현재가(달러)", example = "182.5") double currentPriceUsd,
        @Schema(description = "평가금액(달러) = 현재가 × 수량", example = "1825.0") double valuationUsd) {
}
