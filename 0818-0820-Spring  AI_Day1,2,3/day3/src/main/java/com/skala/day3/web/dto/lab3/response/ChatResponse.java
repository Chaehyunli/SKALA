package com.skala.day3.web.dto.lab3.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 상담 에이전트 채팅 응답(POST /lab3/chat). */
public record ChatResponse(
        @Schema(description = "대화 세션 ID — 다음 요청에 그대로 넣으면 같은 대화로 이어진다.",
                example = "7e85850f-2c47-48ea-a3cf-c5fb3bc76b61")
        String sessionId,

        @Schema(description = "에이전트 답변", example = "주문번호 12345의 무선 이어폰은 현재 배송 중이며, 예상 도착일은 7월 30일입니다.")
        String answer) {
}
