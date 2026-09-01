package com.lecture.course.llm;

import java.util.List;

/** Strict JSON response expected from the first LLM call. */
public record AgentSelectionLlmResponse(List<String> agentCodes) {
}
