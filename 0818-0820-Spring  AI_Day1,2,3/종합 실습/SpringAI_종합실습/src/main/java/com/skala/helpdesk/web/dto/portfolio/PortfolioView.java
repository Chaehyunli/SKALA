package com.skala.helpdesk.web.dto.portfolio;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/** getPortfolio 도구가 모델에 돌려주는 모양 — 현금(원화) + 종목별 평가 + 총자산(원화). */
public record PortfolioView(
        @Schema(description = "보유 현금(원화)", example = "8500000") double cashKrw,
        List<HoldingView> holdings,
        @Schema(description = "현금 + 보유 종목 평가액을 원화로 환산한 총자산", example = "10250000") double totalValuationKrw) {
}
