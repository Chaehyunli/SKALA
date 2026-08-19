package com.skala.day2.web.dto;

public record AskRequest(String question, Integer topK, Double threshold) {

    public AskRequest {
        if (topK == null) {
            topK = 4;
        }
        if (threshold == null) {
            threshold = 0.5;      // 낮은 점수는 근거가 아니다 — 필요하면 요청에서 조정
        }
    }
}
