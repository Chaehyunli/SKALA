package com.skala.helpdesk.web.dto.portfolio;

import io.swagger.v3.oas.annotations.media.Schema;

/** 매수/매도 체결 결과 — complianceTicketNo 는 신고 기준액을 넘겨 티켓이 생성된 경우에만 채워진다. */
public record TradeResultView(
        @Schema(example = "BUY") String side,
        @Schema(example = "AAPL") String symbol,
        @Schema(example = "10") int quantity,
        @Schema(description = "체결 단가(달러)", example = "182.5") double priceUsd,
        @Schema(description = "체결 시점 적용 환율(USD->KRW)", example = "1350.0") double fxRate,
        @Schema(description = "원화 정산 금액(매수: 차감, 매도: 입금)", example = "2463750") double amountKrw,
        @Schema(description = "체결 후 남은 현금(원화)", example = "7536250") double remainingCashKrw,
        @Schema(description = "신고 기준액 초과로 생성된 신고 티켓 번호 — 없으면 null", example = "a1b2c3d4", nullable = true)
        String complianceTicketNo) {
}
