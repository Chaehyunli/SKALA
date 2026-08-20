package com.skala.helpdesk.repository;

import com.skala.helpdesk.domain.Holding;
import com.skala.helpdesk.domain.Portfolio;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.UnaryOperator;

/**
 * 데이터에 닿는 유일한 자리. 실제 DB는 쓰지 않는다 — 서버를 재시작하면 전원 초기 상태로 돌아간다
 * (데모 목적: 사용자별 상태를 매번 새로 쌓지 않고 항상 같은 시작점에서 테스트한다).
 */
@Repository
public class PortfolioRepository {

    private static final double INITIAL_CASH_KRW = 10_000_000;

    private final Map<String, Portfolio> accounts = new ConcurrentHashMap<>();

    /** 이 사용자를 처음 보면 초기 자금·초기 보유 종목으로 계좌를 만든다. */
    public Portfolio findOrCreate(String userId) {
        return accounts.computeIfAbsent(userId, this::seed);
    }

    /** 매수/매도로 바뀐 계좌 스냅샷을 반영한다 — 마지막에 쓴 값이 이긴다(동시 매매 경합은 이번 실습 범위 밖). */
    public Portfolio save(Portfolio portfolio) {
        accounts.put(portfolio.userId(), portfolio);
        return portfolio;
    }

    /** 현재 계좌를 원자적으로 갱신한다 — 조회와 쓰기 사이에 다른 요청이 끼어드는 것을 막는다. */
    public Portfolio update(String userId, UnaryOperator<Portfolio> transition) {
        return accounts.compute(userId, (id, current) -> transition.apply(current != null ? current : seed(id)));
    }

    private Portfolio seed(String userId) {
        return new Portfolio(userId, INITIAL_CASH_KRW, Map.of(
                "AAPL", new Holding("AAPL", 10, 150.0),
                "TSLA", new Holding("TSLA", 5, 200.0)));
    }
}
