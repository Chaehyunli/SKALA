package com.skala.helpdesk.market;

/** 현재가(달러) 스냅샷. */
public record StockQuote(String symbol, double priceUsd) {}
