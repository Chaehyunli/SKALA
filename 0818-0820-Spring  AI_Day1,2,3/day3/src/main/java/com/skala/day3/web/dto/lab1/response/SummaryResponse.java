package com.skala.day3.web.dto.lab1.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 밖으로 나가는 모양. 안쪽 모델(Order)을 그대로 내보내지 않는다. */
public record SummaryResponse(
        @Schema(example = "12345") String orderId,
        @Schema(example = "무선 이어폰이 배송 중이며 7월 30일 도착 예정입니다.") String summary) {}
