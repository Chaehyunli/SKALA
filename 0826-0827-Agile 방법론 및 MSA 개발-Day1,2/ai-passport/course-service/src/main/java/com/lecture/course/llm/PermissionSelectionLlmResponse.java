package com.lecture.course.llm;

import java.util.List;

/** Strict JSON response expected from the second LLM call. */
public record PermissionSelectionLlmResponse(
        List<AgentPermissionDecision> agentList,
        String summary
) {
    public record AgentPermissionDecision(
            String agentCode,
            List<PermissionDecision> permissions,
            List<PermissionDecision> excludedPermissions
    ) {
    }

    public record PermissionDecision(String code, String reason) {
    }
}
