package com.skala.helpdesk.web.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @Schema(description = "미리 등록된 사용자 ID", example = "user1")
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9_-]{3,20}") String userId) {
}
