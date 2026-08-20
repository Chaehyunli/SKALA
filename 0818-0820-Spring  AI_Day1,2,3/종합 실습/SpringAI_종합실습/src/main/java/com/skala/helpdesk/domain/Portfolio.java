package com.skala.helpdesk.domain;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 한 명의 계좌 — 원화 현금과 보유 종목. DB 없이 서버 메모리에만 존재하므로
 * 재시작하면 {@link com.skala.helpdesk.repository.PortfolioRepository} 가 다시 초기값으로 시딩한다.
 *
 * <p>매수/매도의 금액 계산·검증을 이 레코드가 직접 갖는다 — 잔고 부족·수량 부족 판정이
 * 흩어지면 어디선가 마이너스 잔고가 새어나간다.
 */
public record Portfolio(String userId, double cashKrw, Map<String, Holding> holdings) {

    public Portfolio buy(String symbol, int quantity, double priceUsd, double fxRate) {
        double costKrw = quantity * priceUsd * fxRate;
        if (costKrw > cashKrw) {
            throw new IllegalStateException(
                    "현금 잔고가 부족합니다. 필요 금액 약 %,.0f원, 보유 현금 약 %,.0f원".formatted(costKrw, cashKrw));
        }
        Holding existing = holdings.get(symbol);
        Holding updated = existing == null ? new Holding(symbol, quantity, priceUsd) : existing.buy(quantity, priceUsd);
        Map<String, Holding> newHoldings = new HashMap<>(holdings);
        newHoldings.put(symbol, updated);
        return new Portfolio(userId, cashKrw - costKrw, newHoldings);
    }

    public Portfolio sell(String symbol, int quantity, double priceUsd, double fxRate) {
        Holding existing = holdings.get(symbol);
        if (existing == null || existing.quantity() < quantity) {
            int owned = existing == null ? 0 : existing.quantity();
            throw new IllegalStateException(
                    "보유 수량이 부족합니다. 보유 %d주, 매도 요청 %d주".formatted(owned, quantity));
        }
        double proceedsKrw = quantity * priceUsd * fxRate;
        Map<String, Holding> newHoldings = new HashMap<>(holdings);
        if (existing.quantity() == quantity) {
            newHoldings.remove(symbol);
        } else {
            newHoldings.put(symbol, existing.sell(quantity));
        }
        return new Portfolio(userId, cashKrw + proceedsKrw, newHoldings);
    }
}
