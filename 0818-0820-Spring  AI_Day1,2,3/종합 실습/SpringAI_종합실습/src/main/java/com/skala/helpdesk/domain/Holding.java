package com.skala.helpdesk.domain;

/** 종목 하나의 보유 수량·평단가(달러 기준). */
public record Holding(String symbol, int quantity, double avgBuyPriceUsd) {

    public Holding buy(int qty, double priceUsd) {
        int newQuantity = quantity + qty;
        double newAvg = (quantity * avgBuyPriceUsd + qty * priceUsd) / newQuantity;
        return new Holding(symbol, newQuantity, newAvg);
    }

    /** 전량 매도는 호출부(Portfolio)에서 Map 제거로 처리하므로 여기선 항상 잔여 수량이 있다고 가정한다. */
    public Holding sell(int qty) {
        return new Holding(symbol, quantity - qty, avgBuyPriceUsd);
    }
}
