package com.skala.helpdesk.web.dto.chat.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** RAG 답변이 실제로 참조한 규정 문서 출처. */
public record Source(
        @Schema(example = "해외주식-보유-신고-규정") String document,
        @Schema(example = "v1") String version) {
}
