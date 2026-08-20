package com.skala.day3.web.dto.lab2.request;

import io.swagger.v3.oas.annotations.media.Schema;

public record AskRequest(
        String question,
        @Schema(defaultValue = "4") Integer topK,
        @Schema(defaultValue = "0.5") Double threshold) {

    public AskRequest {
        if (topK == null) {
            topK = 4;
        }
        if (threshold == null) {
            threshold = 0.5;      // 낮은 점수는 근거가 아니다 — 필요하면 요청에서 조정
        }
    }
}
