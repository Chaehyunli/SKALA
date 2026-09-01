package com.lecture.course.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lecture.course.agent.AgentCatalog;
import com.lecture.course.agent.AgentDefinition;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** The only class that calls the local Ollama /api/chat endpoint. */
@Component
@RequiredArgsConstructor
public class LlmAnalysisClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final LlmProperties properties;
    private final LlmPromptFactory promptFactory;

    public AgentSelectionLlmResponse selectAgents(AnalysisMission mission, AgentCatalog catalog) {
        String content = requestStructuredJson(
                promptFactory.agentSelectionSystemPrompt(),
                promptFactory.agentSelectionUserPrompt(mission, catalog),
                promptFactory.agentSelectionSchema(catalog));
        return read(content, AgentSelectionLlmResponse.class);
    }

    public PermissionSelectionLlmResponse selectPermissions(
            AnalysisMission mission,
            List<AgentDefinition> selectedAgents
    ) {
        String content = requestStructuredJson(
                promptFactory.permissionSelectionSystemPrompt(),
                promptFactory.permissionSelectionUserPrompt(mission, selectedAgents),
                promptFactory.permissionSelectionSchema(selectedAgents));
        return read(content, PermissionSelectionLlmResponse.class);
    }

    private String requestStructuredJson(
            String systemPrompt,
            String userPrompt,
            Map<String, Object> schema
    ) {
        validateConfiguration();
        Map<String, Object> request = Map.of(
                "model", properties.getModel(),
                "stream", false,
                "think", false,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt + "\n응답 JSON Schema:\n" + writeJson(schema))),
                "format", schema,
                "options", Map.of("temperature", 0));

        try {
            HttpClient httpClient = HttpClient.create()
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.getConnectTimeoutMs())
                    .responseTimeout(Duration.ofMillis(properties.getReadTimeoutMs()));
            OllamaChatResponse response = webClientBuilder
                    .clientConnector(new ReactorClientHttpConnector(httpClient))
                    .build()
                    .post()
                    .uri(properties.getBaseUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(OllamaChatResponse.class)
                    .block(Duration.ofMillis(properties.getReadTimeoutMs()));

            if (response == null || response.message() == null || response.message().content() == null) {
                throw new LlmAnalysisException("LLM이 비어 있는 응답을 반환했습니다");
            }
            return response.message().content();
        } catch (LlmAnalysisException e) {
            throw e;
        } catch (Exception e) {
            throw new LlmAnalysisException("LLM 분석 호출에 실패했습니다", e);
        }
    }

    private <T> T read(String content, Class<T> responseType) {
        try {
            return objectMapper.readValue(content, responseType);
        } catch (JsonProcessingException e) {
            throw new LlmAnalysisException("LLM 구조화 응답을 해석하지 못했습니다", e);
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getBaseUrl()) || isBlank(properties.getModel())) {
            throw new LlmAnalysisException("Ollama 주소와 모델 설정이 필요합니다");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new LlmAnalysisException("LLM JSON Schema를 만들지 못했습니다", e);
        }
    }

    private record OllamaChatResponse(Message message) {
    }

    private record Message(String content) {
    }
}
