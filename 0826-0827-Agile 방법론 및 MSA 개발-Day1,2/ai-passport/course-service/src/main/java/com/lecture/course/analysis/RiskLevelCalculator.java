package com.lecture.course.analysis;

import com.lecture.course.agent.AgentDefinition;
import org.springframework.stereotype.Component;

import java.util.Collection;

/** Maps policy risks to the existing Passport risk contract: LOW, MEDIUM, HIGH. */
@Component
public class RiskLevelCalculator {

    public String calculate(Collection<AgentDefinition.AgentPermissionDefinition> permissions) {
        boolean hasMedium = false;
        for (AgentDefinition.AgentPermissionDefinition permission : permissions) {
            String risk = permission.risk();
            if ("CRITICAL".equalsIgnoreCase(risk) || "HIGH".equalsIgnoreCase(risk)) {
                return "HIGH";
            }
            if ("MEDIUM".equalsIgnoreCase(risk)) {
                hasMedium = true;
            }
        }
        return hasMedium ? "MEDIUM" : "LOW";
    }
}
