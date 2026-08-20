package com.skala.helpdesk.tool;

import com.skala.helpdesk.domain.Holding;
import com.skala.helpdesk.domain.Portfolio;
import com.skala.helpdesk.market.ExchangeRate;
import com.skala.helpdesk.market.PriceService;
import com.skala.helpdesk.market.StockQuote;
import com.skala.helpdesk.repository.ComplianceTicketRepository;
import com.skala.helpdesk.repository.PortfolioRepository;
import com.skala.helpdesk.service.ComplianceMailService;
import com.skala.helpdesk.service.ToolCallRecorder;
import com.skala.helpdesk.web.dto.portfolio.HoldingView;
import com.skala.helpdesk.web.dto.portfolio.PortfolioView;
import com.skala.helpdesk.web.dto.portfolio.TradeResultView;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 모델에 노출되는 매매 도구. 사용자 식별은 파라미터가 아니라 ToolContext 로만 받는다 —
 * 모델이 프롬프트로 다른 사용자의 userId 를 흉내 내도 여기엔 반영되지 않는다(day3 OrderTools 원칙 그대로).
 *
 * <p>매수/매도 자체는 즉시 체결한다 — 승인 절차는 신고 기준액을 넘긴 경우의
 * "신고 티켓"에만 걸린다({@link ComplianceTicketRepository}). 체결을 막는 게 아니라
 * 신고 의무 확인만 사람이 승인하는 구조다.
 */
@Component
public class PortfolioTools {

    private final PortfolioRepository portfolios;
    private final ComplianceTicketRepository tickets;
    private final ComplianceMailService mailService;
    private final PriceService priceService;
    private final ToolCallRecorder recorder;
    private final double reportThresholdUsd;

    public PortfolioTools(PortfolioRepository portfolios,
                           ComplianceTicketRepository tickets,
                           ComplianceMailService mailService,
                           PriceService priceService,
                           ToolCallRecorder recorder,
                           @Value("${helpdesk.compliance.report-threshold-usd}") double reportThresholdUsd) {
        this.portfolios = portfolios;
        this.tickets = tickets;
        this.mailService = mailService;
        this.priceService = priceService;
        this.recorder = recorder;
        this.reportThresholdUsd = reportThresholdUsd;
    }

    @Tool(description = """
            보유 현금과 보유 종목별 평가금액, 총자산을 조회한다.
            '내 포트폴리오', '보유 주식', '잔고 얼마야' 같은 질문에 쓴다.
            """)
    public PortfolioView getPortfolio(ToolContext context) {
        return recorder.execute("getPortfolio", () -> {
            Portfolio portfolio = portfolios.findOrCreate(userId(context));
            List<HoldingView> views = portfolio.holdings().values().stream()
                    .map(this::toHoldingView)
                    .toList();
            double totalStockUsd = views.stream().mapToDouble(HoldingView::valuationUsd).sum();
            double fx = totalStockUsd > 0 ? priceService.getRate("USD", "KRW").rate() : 0;
            double totalValuationKrw = portfolio.cashKrw() + totalStockUsd * fx;
            return new PortfolioView(portfolio.cashKrw(), views, totalValuationKrw);
        });
    }

    @Tool(description = """
            해외 주식을 매수한다. 실시간 시세·환율로 필요 금액을 계산해 즉시 체결하며,
            현금 잔고가 부족하면 거부한다. 매수 후 전체 보유 종목의 현재 평가액 합계가 신고 기준액을 넘으면
            신고 티켓이 자동 생성되고 담당자 승인 절차를 안내해야 한다.
            """)
    public TradeResultView buyStock(@ToolParam(description = "종목 코드. 예: AAPL") String symbol,
                                     @ToolParam(description = "매수 수량(주)") int quantity,
                                     ToolContext context) {
        return recorder.execute("buyStock", () -> {
            requirePositive(quantity);
            String userId = userId(context);
            StockQuote quote = priceService.getQuote(symbol);
            ExchangeRate fx = priceService.getRate("USD", "KRW");

            Portfolio updated = portfolios.update(userId, p -> p.buy(quote.symbol(), quantity, quote.priceUsd(), fx.rate()));
            double amountKrw = quantity * quote.priceUsd() * fx.rate();

            String ticketNo = maybeCreateComplianceTicket(userId, updated);
            return new TradeResultView("BUY", quote.symbol(), quantity, quote.priceUsd(), fx.rate(),
                    amountKrw, updated.cashKrw(), ticketNo);
        });
    }

    @Tool(description = """
            보유 중인 해외 주식을 매도한다. 실시간 시세로 즉시 체결하고 원화로 정산하며,
            보유 수량보다 많이 팔려고 하면 거부한다.
            """)
    public TradeResultView sellStock(@ToolParam(description = "종목 코드. 예: AAPL") String symbol,
                                      @ToolParam(description = "매도 수량(주)") int quantity,
                                      ToolContext context) {
        return recorder.execute("sellStock", () -> {
            requirePositive(quantity);
            String userId = userId(context);
            StockQuote quote = priceService.getQuote(symbol);
            ExchangeRate fx = priceService.getRate("USD", "KRW");

            Portfolio updated = portfolios.update(userId, p -> p.sell(quote.symbol(), quantity, quote.priceUsd(), fx.rate()));
            double amountKrw = quantity * quote.priceUsd() * fx.rate();

            return new TradeResultView("SELL", quote.symbol(), quantity, quote.priceUsd(), fx.rate(),
                    amountKrw, updated.cashKrw(), null);
        });
    }

    /** userId 별로 원자적으로 생성되므로(ComplianceTicketRepository) 동시 매수에도 중복 티켓·중복 메일이 생기지 않는다. */
    private String maybeCreateComplianceTicket(String userId, Portfolio updated) {
        double valuationUsd = updated.holdings().values().stream()
                .mapToDouble(holding -> holding.quantity() * priceService.getQuote(holding.symbol()).priceUsd())
                .sum();
        if (valuationUsd < reportThresholdUsd) {
            return null;
        }
        var result = tickets.createPendingIfAbsent(userId, "PORTFOLIO", valuationUsd);
        if (result.created()) {
            mailService.notify(result.ticket());
        }
        return result.ticket().no();
    }

    private HoldingView toHoldingView(Holding holding) {
        double currentPriceUsd = priceService.getQuote(holding.symbol()).priceUsd();
        return new HoldingView(holding.symbol(), holding.quantity(), holding.avgBuyPriceUsd(),
                currentPriceUsd, currentPriceUsd * holding.quantity());
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("수량은 1주 이상이어야 합니다.");
        }
    }

    private String userId(ToolContext context) {
        return (String) context.getContext().get("userId");
    }
}
