package com.lecture.course.service;

import com.lecture.course.agent.AgentCatalog;
import com.lecture.course.agent.AgentCatalogLoader;
import com.lecture.course.agent.AgentDefinition;
import com.lecture.course.analysis.AnalysisResultValidator;
import com.lecture.course.analysis.ResolvedAnalysisResult;
import com.lecture.course.llm.AnalysisMission;
import com.lecture.course.llm.AgentSelectionLlmResponse;
import com.lecture.course.llm.LlmAnalysisClient;
import com.lecture.course.llm.PermissionSelectionLlmResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** Coordinates the two LLM calls without owning Course persistence or HTTP endpoints. */
@Service
@RequiredArgsConstructor
public class LlmCourseAnalysisService {

    private final AgentCatalogLoader agentCatalogLoader;
    private final LlmAnalysisClient llmAnalysisClient;
    private final AnalysisResultValidator analysisResultValidator;

    public ResolvedAnalysisResult analyze(AnalysisMission mission) {
        AgentCatalog catalog = agentCatalogLoader.getCatalog();

        AgentSelectionLlmResponse selected = llmAnalysisClient.selectAgents(mission, catalog);
        List<AgentDefinition> selectedAgents = analysisResultValidator.validateAgentSelection(selected, catalog);

        PermissionSelectionLlmResponse permissionSelection =
                llmAnalysisClient.selectPermissions(mission, selectedAgents);
        return analysisResultValidator.validatePermissionSelection(permissionSelection, selectedAgents);
    }
}
