package com.skala.day1.web;

/** 상세는 로그에, 사용자에게는 추적 ID만. */
public record ErrorResponse(String message, String traceId) {}
