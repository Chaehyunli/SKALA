package com.skala.helpdesk.market;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 외부 시세 API를 감싸는 유일한 자리 — Finnhub(주가)·Frankfurter(환율).
 * 독립 Tool(StockQuoteTools·ExchangeRateTools)과 매수/매도 계산(PortfolioTools) 양쪽이
 * 이 서비스 하나만 호출한다. 호출부마다 각자 API를 파싱하게 두면 응답 스키마가 바뀔 때
 * 여러 곳을 고쳐야 한다.
 */
@Component
public class PriceService {

    private final RestClient finnhubClient;
    private final RestClient exchangeClient;
    private final String finnhubApiKey;

    public PriceService(@Value("${market.finnhub.base-url}") String finnhubBaseUrl,
                         @Value("${market.finnhub.api-key}") String finnhubApiKey,
                         @Value("${market.exchange.base-url}") String exchangeBaseUrl) {
        this.finnhubClient = RestClient.builder().baseUrl(finnhubBaseUrl).build();
        this.exchangeClient = RestClient.builder().baseUrl(exchangeBaseUrl).build();
        this.finnhubApiKey = finnhubApiKey;
    }

    public StockQuote getQuote(String symbol) {
        if (finnhubApiKey == null || finnhubApiKey.isBlank()) {
            throw new IllegalStateException("FINNHUB_API_KEY가 설정되지 않았습니다. .env를 확인하세요.");
        }
        String upper = symbol.toUpperCase();
        Map<String, Object> body = finnhubClient.get()
                .uri(uri -> uri.path("/quote").queryParam("symbol", upper).queryParam("token", finnhubApiKey).build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        double price = body == null ? 0 : ((Number) body.getOrDefault("c", 0)).doubleValue();
        if (price <= 0) {
            throw new IllegalArgumentException("존재하지 않거나 시세를 조회할 수 없는 종목입니다: " + upper);
        }
        return new StockQuote(upper, price);
    }

    @SuppressWarnings("unchecked")
    public ExchangeRate getRate(String base, String quote) {
        String upperBase = base.toUpperCase();
        String upperQuote = quote.toUpperCase();
        Map<String, Object> body = exchangeClient.get()
                .uri(uri -> uri.path("/latest").queryParam("base", upperBase).queryParam("symbols", upperQuote).build())
                .retrieve()
                .body(new ParameterizedTypeReference<Map<String, Object>>() {});
        Map<String, Object> rates = body == null ? Map.of() : (Map<String, Object>) body.getOrDefault("rates", Map.of());
        Number rate = (Number) rates.get(upperQuote);
        if (rate == null) {
            throw new IllegalArgumentException("환율을 조회할 수 없는 통화입니다: " + upperBase + "->" + upperQuote);
        }
        return new ExchangeRate(upperBase, upperQuote, rate.doubleValue());
    }
}
