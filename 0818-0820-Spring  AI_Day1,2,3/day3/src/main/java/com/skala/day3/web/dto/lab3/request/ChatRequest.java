package com.skala.day3.web.dto.lab3.request;

import io.swagger.v3.oas.annotations.media.Schema;

/** 상담 에이전트 채팅 요청(POST /lab3/chat) 바디. */
public record ChatRequest(
        @Schema(description = "요청자 ID — 본인 소유 주문만 조회·환불 접수된다.", example = "user1")
        String userId,

        @Schema(description = "대화 세션 ID. 생략하면 이 userId 의 기본 대화로 자동으로 이어진다(매번 안 넣어도 기억함). "
                + "여러 대화를 동시에 유지하고 싶을 때만 원하는 값을 직접 지어서 보낸다 — 그 값으로 새로 격리된 대화가 시작된다.",
                example = "", nullable = true)
        String sessionId,

        @Schema(description = "사용자 메시지", example = "제 주문 12345는 지금 어디예요?")
        String message) {
}
