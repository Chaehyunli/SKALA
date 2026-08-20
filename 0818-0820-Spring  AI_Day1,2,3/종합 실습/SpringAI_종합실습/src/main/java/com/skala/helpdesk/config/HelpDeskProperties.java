package com.skala.helpdesk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** helpdesk.* 설정 — 공급자·모델·임계값을 코드에 상수로 남기지 않는다. */
@ConfigurationProperties(prefix = "helpdesk")
public record HelpDeskProperties(Rag rag, Memory memory) {

    public record Rag(int topK, double threshold) {}

    public record Memory(int maxMessages) {}
}
