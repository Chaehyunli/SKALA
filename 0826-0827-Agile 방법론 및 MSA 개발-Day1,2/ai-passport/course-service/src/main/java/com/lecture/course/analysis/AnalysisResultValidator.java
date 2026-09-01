package com.lecture.course.analysis;

import com.lecture.course.agent.AgentCatalog;
import com.lecture.course.agent.AgentDefinition;
import com.lecture.course.llm.AgentSelectionLlmResponse;
import com.lecture.course.llm.PermissionSelectionLlmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Treats LLM output as untrusted and verifies it against the bundled Agent policies. */
@Component
@RequiredArgsConstructor
public class AnalysisResultValidator {

    private final RiskLevelCalculator riskLevelCalculator;

    public List<AgentDefinition> validateAgentSelection(
            AgentSelectionLlmResponse response,
            AgentCatalog catalog
    ) {
        if (response == null || response.agentCodes() == null || response.agentCodes().isEmpty()) {
            throw invalid("최소 한 개의 Agent를 선택해야 합니다");
        }

        Set<String> seen = new LinkedHashSet<>();
        List<AgentDefinition> selectedAgents = new ArrayList<>();
        for (String agentCode : response.agentCodes()) {
            if (isBlank(agentCode) || !seen.add(agentCode)) {
                throw invalid("Agent 코드가 비어 있거나 중복되었습니다");
            }
            AgentDefinition agent = catalog.requireAgent(agentCode);
            if (!agent.selectable()) {
                throw invalid("선택할 수 없는 Agent입니다: " + agentCode);
            }
            selectedAgents.add(agent);
        }
        return List.copyOf(selectedAgents);
    }

    public ResolvedAnalysisResult validatePermissionSelection(
            PermissionSelectionLlmResponse response,
            List<AgentDefinition> selectedAgents
    ) {
        if (response == null || response.agentList() == null || isBlank(response.summary())) {
            throw invalid("Agent별 권한 결과 또는 요약이 없습니다");
        }

        Map<String, PermissionSelectionLlmResponse.AgentPermissionDecision> resultByAgent = new LinkedHashMap<>();
        for (PermissionSelectionLlmResponse.AgentPermissionDecision result : response.agentList()) {
            if (result == null || isBlank(result.agentCode()) || resultByAgent.putIfAbsent(result.agentCode(), result) != null) {
                throw invalid("Agent별 권한 결과가 비어 있거나 중복되었습니다");
            }
        }

        Set<String> selectedCodes = selectedAgents.stream()
                .map(AgentDefinition::agentCode)
                .collect(java.util.stream.Collectors.toSet());
        if (!resultByAgent.keySet().equals(selectedCodes)) {
            throw invalid("2차 분석의 Agent 목록이 1차 선택 결과와 일치하지 않습니다");
        }

        List<ResolvedAnalysisResult.AgentAnalysis> agentAnalyses = new ArrayList<>();
        List<AgentDefinition.AgentPermissionDefinition> grantedPermissions = new ArrayList<>();
        for (AgentDefinition agent : selectedAgents) {
            PermissionSelectionLlmResponse.AgentPermissionDecision result = resultByAgent.get(agent.agentCode());
            Map<String, AgentDefinition.AgentPermissionDefinition> catalogPermissions = agent.permissionsByCode();
            Map<String, String> granted = decisionsToReasons(result.permissions(), "permissions", agent.agentCode());
            Map<String, String> excluded = decisionsToReasons(result.excludedPermissions(), "excludedPermissions", agent.agentCode());

            Set<String> overlap = new HashSet<>(granted.keySet());
            overlap.retainAll(excluded.keySet());
            if (!overlap.isEmpty()) {
                throw invalid("허용/제외 권한이 중복되었습니다: " + agent.agentCode());
            }

            // The model selects only the minimal required permissions. Any policy permission
            // it omits is safely represented as excluded instead of granting it implicitly.
            // Unknown codes are still rejected below while resolving the decision.
            Set<String> classified = new HashSet<>();
            classified.addAll(granted.keySet());
            classified.addAll(excluded.keySet());
            for (String permissionCode : catalogPermissions.keySet()) {
                if (!classified.contains(permissionCode)) {
                    excluded.put(permissionCode, "업무 수행에 직접 필요하지 않아 최소 권한 원칙에 따라 제외합니다.");
                }
            }
            if (granted.isEmpty()) {
                throw invalid("선택된 Agent에는 최소 한 개의 허용 권한이 필요합니다: " + agent.agentCode());
            }

            List<ResolvedAnalysisResult.AnalysisPermission> resolvedGranted = resolvePermissions(
                    granted, catalogPermissions, grantedPermissions);
            List<ResolvedAnalysisResult.AnalysisPermission> resolvedExcluded = resolvePermissions(
                    excluded, catalogPermissions, null);
            agentAnalyses.add(new ResolvedAnalysisResult.AgentAnalysis(
                    agent.agentCode(), resolvedGranted, resolvedExcluded));
        }

        return new ResolvedAnalysisResult(
                List.copyOf(agentAnalyses),
                riskLevelCalculator.calculate(grantedPermissions),
                response.summary().trim());
    }

    private Map<String, String> decisionsToReasons(
            List<PermissionSelectionLlmResponse.PermissionDecision> decisions,
            String field,
            String agentCode
    ) {
        Map<String, String> reasons = new LinkedHashMap<>();
        if (decisions == null) {
            return reasons;
        }
        for (PermissionSelectionLlmResponse.PermissionDecision decision : decisions) {
            if (decision == null || isBlank(decision.code()) || isBlank(decision.reason())
                    || reasons.putIfAbsent(decision.code(), decision.reason().trim()) != null) {
                throw invalid("%s에 유효하지 않거나 중복된 권한이 있습니다: %s".formatted(field, agentCode));
            }
        }
        return reasons;
    }

    private List<ResolvedAnalysisResult.AnalysisPermission> resolvePermissions(
            Map<String, String> reasons,
            Map<String, AgentDefinition.AgentPermissionDefinition> catalogPermissions,
            Collection<AgentDefinition.AgentPermissionDefinition> grantedPermissions
    ) {
        List<ResolvedAnalysisResult.AnalysisPermission> resolved = new ArrayList<>();
        reasons.forEach((code, reason) -> {
            AgentDefinition.AgentPermissionDefinition permission = catalogPermissions.get(code);
            if (permission == null) {
                throw invalid("Agent YAML에 없는 권한입니다: " + code);
            }
            resolved.add(new ResolvedAnalysisResult.AnalysisPermission(
                    code,
                    permission.label(),
                    permission.description(),
                    reason));
            if (grantedPermissions != null) {
                grantedPermissions.add(permission);
            }
        });
        return List.copyOf(resolved);
    }

    private IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException("LLM 분석 결과가 정책에 맞지 않습니다: " + message);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
