package com.skala.helpdesk.web.dto.chat.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record AnswerDto(
        String answer,
        @Schema(description = "답변이 인용한 규정 문서 출처 — 근거를 못 찾았으면 빈 배열") List<Source> sources,
        @Schema(description = "이번 턴에서 매수/매도/포트폴리오 조회 등 도구가 실행됐는지") boolean toolUsed) {
}
