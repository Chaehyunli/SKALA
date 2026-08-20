package com.skala.helpdesk.web.dto.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AskRequest(
        @Schema(description = "로그인한 사용자 ID와 같아야 한다", example = "user1")
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{3,20}") String userId,
        @Schema(example = "제 포트폴리오 보여주세요") @NotBlank @Size(max = 1_000) String message,
        @Schema(description = "생략하면 기본 대화로 이어진다. 사용자 ID와 합쳐 JDBC 대화 ID를 구성한다.", example = "user1Session", nullable = true)
        @Pattern(regexp = "[a-zA-Z0-9_-]{1,15}") String sessionId) {
}
