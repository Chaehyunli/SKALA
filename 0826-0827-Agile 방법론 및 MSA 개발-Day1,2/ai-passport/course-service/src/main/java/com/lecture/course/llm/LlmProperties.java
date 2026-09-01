package com.lecture.course.llm;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {
    private String baseUrl = "http://localhost:11434/api/chat";
    private String model = "qwen3.5";
    private int connectTimeoutMs = 5000;
    private int readTimeoutMs = 30000;
}
