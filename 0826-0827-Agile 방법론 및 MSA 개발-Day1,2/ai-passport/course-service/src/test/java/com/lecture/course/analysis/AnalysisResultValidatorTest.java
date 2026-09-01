package com.lecture.course.analysis;

import com.lecture.course.agent.AgentCatalog;
import com.lecture.course.agent.AgentDefinition;
import com.lecture.course.llm.AgentSelectionLlmResponse;
import com.lecture.course.llm.PermissionSelectionLlmResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AnalysisResultValidatorTest {

    private final AnalysisResultValidator validator = new AnalysisResultValidator(new RiskLevelCalculator());

    @Test
    void enrichesYamlNamesAndCalculatesCriticalAsHigh() {
        AgentDefinition agent = agent();
        List<AgentDefinition> selected = validator.validateAgentSelection(
                new AgentSelectionLlmResponse(List.of("COMMON_AGENT")),
                new AgentCatalog(Map.of("COMMON_AGENT", agent)));

        ResolvedAnalysisResult result = validator.validatePermissionSelection(
                new PermissionSelectionLlmResponse(List.of(
                        new PermissionSelectionLlmResponse.AgentPermissionDecision(
                                "COMMON_AGENT",
                                List.of(new PermissionSelectionLlmResponse.PermissionDecision(
                                        "DOCUMENT_DELETE", "삭제가 명시적으로 필요합니다.")),
                                List.of(new PermissionSelectionLlmResponse.PermissionDecision(
                                        "DOCUMENT_READ", "이번 업무에는 조회가 필요하지 않습니다.")))),
                        "문서 삭제 업무입니다."),
                selected);

        assertThat(result.riskLevel()).isEqualTo("HIGH");
        assertThat(result.agentList().getFirst().permissions().getFirst().label())
                .isEqualTo("문서 삭제");
        assertThat(result.agentList().getFirst().permissions().getFirst().description())
                .isEqualTo("문서를 삭제합니다.");
    }

    @Test
    void defaultsPermissionsOmittedByTheModelToExcluded() {
        AgentDefinition agent = agent();
        ResolvedAnalysisResult result = validator.validatePermissionSelection(
                new PermissionSelectionLlmResponse(List.of(
                        new PermissionSelectionLlmResponse.AgentPermissionDecision(
                                "COMMON_AGENT",
                                List.of(new PermissionSelectionLlmResponse.PermissionDecision(
                                        "DOCUMENT_READ", "문서 내용을 확인해야 합니다.")),
                                List.of())),
                        "문서 조회 업무입니다."),
                List.of(agent));

        assertThat(result.agentList().getFirst().permissions())
                .extracting(ResolvedAnalysisResult.AnalysisPermission::code)
                .containsExactly("DOCUMENT_READ");
        assertThat(result.agentList().getFirst().excludedPermissions())
                .extracting(ResolvedAnalysisResult.AnalysisPermission::code)
                .containsExactly("DOCUMENT_DELETE");
    }

    private AgentDefinition agent() {
        AgentDefinition.AgentPermissionDefinition low = new AgentDefinition.AgentPermissionDefinition(
                "문서 조회", "문서를 읽습니다.", "LOW", true, false, false, false, 24);
        AgentDefinition.AgentPermissionDefinition critical = new AgentDefinition.AgentPermissionDefinition(
                "문서 삭제", "문서를 삭제합니다.", "CRITICAL", false, false, true, false, 2);
        return new AgentDefinition(
                "COMMON_AGENT", "Common Agent", "공통 업무", true, "문서 업무", List.of("문서 처리"),
                Map.of("DOCUMENT", new AgentDefinition.AgentGroup(
                        "문서", "문서 권한", Map.of("DOCUMENT_READ", low, "DOCUMENT_DELETE", critical))));
    }
}
