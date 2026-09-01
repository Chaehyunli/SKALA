package com.lecture.course.agent;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Loads the versioned Agent policies that are packaged with course-service. */
@Component
public class AgentCatalogLoader {

    private static final String AGENT_RESOURCE_PATTERN = "classpath*:agents/*.yml";

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    private volatile AgentCatalog catalog;

    @PostConstruct
    void loadCatalog() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources(AGENT_RESOURCE_PATTERN);
            if (resources.length == 0) {
                throw new IllegalStateException("Agent 정책 YAML을 찾을 수 없습니다: " + AGENT_RESOURCE_PATTERN);
            }

            Map<String, AgentDefinition> agents = new LinkedHashMap<>();
            Arrays.stream(resources)
                    .sorted(Comparator.comparing(Resource::getFilename))
                    .forEach(resource -> loadResource(resource, agents));

            catalog = new AgentCatalog(agents);
        } catch (IOException e) {
            throw new IllegalStateException("Agent 정책 YAML 로드에 실패했습니다", e);
        }
    }

    public AgentCatalog getCatalog() {
        return catalog;
    }

    private void loadResource(Resource resource, Map<String, AgentDefinition> agents) {
        try (var input = resource.getInputStream()) {
            AgentDefinition agent = yamlMapper.readValue(input, AgentDefinition.class);
            validate(agent, resource.getFilename());
            if (agents.putIfAbsent(agent.agentCode(), agent) != null) {
                throw new IllegalStateException("중복 Agent 코드입니다: " + agent.agentCode());
            }
        } catch (IOException e) {
            throw new IllegalStateException("Agent 정책을 읽을 수 없습니다: " + resource.getFilename(), e);
        }
    }

    private void validate(AgentDefinition agent, String filename) {
        if (agent == null || isBlank(agent.agentCode()) || isBlank(agent.name()) || agent.groups() == null) {
            throw new IllegalStateException("유효하지 않은 Agent 정책 파일입니다: " + filename);
        }
        agent.permissionsByCode().forEach((code, permission) -> {
            if (isBlank(code) || permission == null || isBlank(permission.label()) || isBlank(permission.risk())) {
                throw new IllegalStateException("유효하지 않은 권한 정책입니다: " + filename + ", " + code);
            }
        });
        if (agent.permissionsByCode().isEmpty()) {
            throw new IllegalStateException("권한이 없는 Agent 정책입니다: " + filename);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
