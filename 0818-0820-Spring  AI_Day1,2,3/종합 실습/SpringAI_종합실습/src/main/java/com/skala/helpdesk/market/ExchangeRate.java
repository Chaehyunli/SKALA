package com.skala.helpdesk.market;

/** base 통화 1단위가 quote 통화로 얼마인지. */
public record ExchangeRate(String base, String quote, double rate) {}
