package com.lecture.course.agent;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Canonical in-memory representation of one resources/agents/*.yml policy file. */
public record AgentDefinition(
        @JsonProperty("agent") String agentCode,
        String name,
        String task,
        boolean selectable,
        String description,
        List<String> typicalTasks,
        Map<String, AgentGroup> groups
) {
    public Map<String, AgentPermissionDefinition> permissionsByCode() {
        Map<String, AgentPermissionDefinition> permissions = new LinkedHashMap<>();
        if (groups == null) {
            return permissions;
        }
        groups.forEach((groupCode, group) -> {
            if (group != null && group.permissions() != null) {
                group.permissions().forEach((permissionCode, permission) -> {
                    if (permissions.putIfAbsent(permissionCode, permission) != null) {
                        throw new IllegalStateException("Agent %s has duplicate permission %s"
                                .formatted(agentCode, permissionCode));
                    }
                });
            }
        });
        return permissions;
    }

    public record AgentGroup(
            String displayName,
            String description,
            Map<String, AgentPermissionDefinition> permissions
    ) {
    }

    public record AgentPermissionDefinition(
            String label,
            String description,
            String risk,
            boolean reversible,
            boolean externalImpact,
            boolean humanReviewRequired,
            boolean containsPersonalData,
            Integer recommendedTtlHours
    ) {
    }
}
