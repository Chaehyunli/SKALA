package com.skala.day3.web.dto.lab3.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** 대화 이력 조회(GET /lab3/chat/history) 응답 한 건. */
public record ChatMessageView(
        @Schema(description = "발화자 — user(사용자) 또는 assistant(에이전트)", example = "user")
        String role,

        @Schema(description = "메시지 내용", example = "제 주문 12345는 지금 어디예요?")
        String content) {
}
