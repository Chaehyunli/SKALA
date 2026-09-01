package com.lecture.course.analysis;

import java.util.List;

/** Validated, YAML-enriched result ready for the Course persistence model. */
public record ResolvedAnalysisResult(
        List<AgentAnalysis> agentList,
        String riskLevel,
        String summary
) {
    public record AgentAnalysis(
            String agentCode,
            List<AnalysisPermission> permissions,
            List<AnalysisPermission> excludedPermissions
    ) {
    }

    public record AnalysisPermission(String code, String label, String description, String reason) {
    }
}
