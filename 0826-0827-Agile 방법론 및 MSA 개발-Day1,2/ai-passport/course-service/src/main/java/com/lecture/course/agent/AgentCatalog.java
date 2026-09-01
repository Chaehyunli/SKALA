package com.lecture.course.agent;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Read-only Agent policy catalog loaded from the bundled YAML files. */
public record AgentCatalog(Map<String, AgentDefinition> agentsByCode) {
    public AgentCatalog {
        agentsByCode = Map.copyOf(agentsByCode);
    }

    public AgentDefinition requireAgent(String agentCode) {
        AgentDefinition agent = agentsByCode.get(agentCode);
        if (agent == null) {
            throw new IllegalArgumentException("정의되지 않은 Agent 코드입니다: " + agentCode);
        }
        return agent;
    }

    public Collection<AgentDefinition> selectableAgents() {
        return agentsByCode.values().stream().filter(AgentDefinition::selectable).toList();
    }

    public List<String> selectableAgentCodes() {
        return selectableAgents().stream().map(AgentDefinition::agentCode).toList();
    }
}
