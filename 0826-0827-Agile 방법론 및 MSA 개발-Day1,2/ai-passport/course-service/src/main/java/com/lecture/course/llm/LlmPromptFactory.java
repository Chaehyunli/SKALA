package com.lecture.course.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecture.course.agent.AgentCatalog;
import com.lecture.course.agent.AgentDefinition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Creates provider-neutral prompts and JSON-schema response constraints. */
@Component
@RequiredArgsConstructor
public class LlmPromptFactory {

    private final ObjectMapper objectMapper;

    public String agentSelectionSystemPrompt() {
        return """
                당신은 사내 업무용 Agent 선택기입니다. 제공된 업무와 Agent 카탈로그만 근거로,
                업무 수행에 실제로 필요한 Agent를 하나 이상 선택하세요. 여러 Agent가 모두 필요한 경우에만 복수 선택합니다.
                권한을 선택하거나 추론 결과를 설명하지 마세요. 제공된 Agent 코드만 사용하고 JSON 스키마에 맞는 JSON만 반환하세요.
                """;
    }

    public String agentSelectionUserPrompt(AnalysisMission mission, AgentCatalog catalog) {
        List<Map<String, Object>> agents = catalog.selectableAgents().stream()
                .map(agent -> Map.<String, Object>of(
                        "agentCode", agent.agentCode(),
                        "name", agent.name(),
                        "task", agent.task(),
                        "description", agent.description(),
                        "typicalTasks", agent.typicalTasks() == null ? List.of() : agent.typicalTasks()))
                .toList();
        return toJson(Map.of("mission", mission, "agents", agents));
    }

    public Map<String, Object> agentSelectionSchema(AgentCatalog catalog) {
        return objectSchema(
                List.of("agentCodes"),
                Map.of("agentCodes", Map.of(
                        "type", "array",
                        "minItems", 1,
                        "uniqueItems", true,
                        "items", Map.of("type", "string", "enum", catalog.selectableAgentCodes()))));
    }

    public String permissionSelectionSystemPrompt() {
        return """
                당신은 최소 권한 원칙을 적용하는 사내 업무 권한 설계기입니다.
                선택된 Agent마다 업무 수행에 꼭 필요한 최소 권한만 permissions에 넣으세요.
                permissions에 넣지 않은 YAML 권한은 시스템이 자동으로 제외합니다.
                변경, 삭제, 외부 공유, 직접 발송, 반출 권한은 업무에 명시적으로 필요할 때만 허용하세요.
                제공된 Agent와 권한 코드 이외의 값을 만들지 말고, 선택한 모든 항목에 한국어 사유를 작성하세요.
                JSON 스키마에 맞는 JSON만 반환하세요.
                """;
    }

    public String permissionSelectionUserPrompt(AnalysisMission mission, List<AgentDefinition> selectedAgents) {
        List<Map<String, Object>> agents = selectedAgents.stream().map(agent -> {
            List<Map<String, Object>> permissions = new ArrayList<>();
            agent.permissionsByCode().forEach((code, permission) -> permissions.add(Map.of(
                    "code", code,
                    "name", permission.label(),
                    "description", permission.description(),
                    "risk", permission.risk(),
                    "externalImpact", permission.externalImpact(),
                    "humanReviewRequired", permission.humanReviewRequired(),
                    "containsPersonalData", permission.containsPersonalData())));
            return Map.<String, Object>of(
                    "agentCode", agent.agentCode(),
                    "name", agent.name(),
                    "permissions", permissions);
        }).toList();
        return toJson(Map.of("mission", mission, "selectedAgents", agents));
    }

    public Map<String, Object> permissionSelectionSchema(List<AgentDefinition> selectedAgents) {
        List<Map<String, Object>> agentDecisionSchemas = selectedAgents.stream()
                .map(this::agentPermissionDecisionSchema)
                .toList();

        return objectSchema(
                List.of("agentList", "summary"),
                Map.of(
                        "agentList", Map.of("type", "array", "minItems", 1,
                                "items", Map.of("oneOf", agentDecisionSchemas)),
                        "summary", Map.of("type", "string", "minLength", 1)));
    }

    /** Keeps each Agent decision constrained to that Agent's own YAML permission codes. */
    private Map<String, Object> agentPermissionDecisionSchema(AgentDefinition agent) {
        List<String> permissionCodes = List.copyOf(agent.permissionsByCode().keySet());
        Map<String, Object> permissionDecision = objectSchema(
                List.of("code", "reason"),
                Map.of(
                        "code", Map.of("type", "string", "enum", permissionCodes),
                        "reason", Map.of("type", "string", "minLength", 1)));
        return objectSchema(
                List.of("agentCode", "permissions"),
                Map.of(
                        "agentCode", Map.of("type", "string", "enum", List.of(agent.agentCode())),
                        "permissions", Map.of("type", "array", "minItems", 1, "items", permissionDecision)));
    }

    private Map<String, Object> objectSchema(List<String> required, Map<String, Object> properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("required", required);
        schema.put("properties", properties);
        return schema;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new LlmAnalysisException("LLM 요청을 JSON으로 만들지 못했습니다", e);
        }
    }
}
