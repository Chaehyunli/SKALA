package com.skala.helpdesk.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 매수/매도 금액 계산·검증이 핵심 로직이라 이것만 짚어둔다 — 나머지는 얇은 위임이다. */
class PortfolioTest {

    @Test
    void 매수하면_현금이_줄고_평단가가_가중평균으로_갱신된다() {
        Portfolio portfolio = new Portfolio("user1", 5_000_000,
                Map.of("AAPL", new Holding("AAPL", 10, 100.0)));

        Portfolio updated = portfolio.buy("AAPL", 10, 200.0, 1000.0);   // 10주 × $200 × 1000원 = 2,000,000원

        assertEquals(3_000_000, updated.cashKrw());
        assertEquals(20, updated.holdings().get("AAPL").quantity());
        assertEquals(150.0, updated.holdings().get("AAPL").avgBuyPriceUsd()); // (10*100+10*200)/20
    }

    @Test
    void 현금이_부족하면_매수가_거부된다() {
        Portfolio portfolio = new Portfolio("user1", 100_000, Map.of());

        assertThrows(IllegalStateException.class,
                () -> portfolio.buy("AAPL", 10, 200.0, 1000.0));         // 필요 금액 2,000,000원 > 보유 100,000원
    }

    @Test
    void 보유수량보다_많이_팔면_거부된다() {
        Portfolio portfolio = new Portfolio("user1", 0, Map.of("AAPL", new Holding("AAPL", 5, 100.0)));

        assertThrows(IllegalStateException.class,
                () -> portfolio.sell("AAPL", 10, 200.0, 1000.0));
    }

    @Test
    void 전량_매도하면_보유종목에서_사라진다() {
        Portfolio portfolio = new Portfolio("user1", 0, Map.of("AAPL", new Holding("AAPL", 5, 100.0)));

        Portfolio updated = portfolio.sell("AAPL", 5, 200.0, 1000.0);

        assertFalse(updated.holdings().containsKey("AAPL"));
        assertEquals(5 * 200.0 * 1000.0, updated.cashKrw());
    }
}
