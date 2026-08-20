package com.skala.day3.config;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 문서 Q&A(Lab2) 전용 VectorStore 빈.
 *
 * <p>SimpleVectorStore 는 임베딩을 JVM 메모리에 들고 있는 가장 단순한 구현이다.
 * EmbeddingModel(OpenAI text-embedding-3-small)은 spring-ai-starter-model-openai 가
 * application.yml 의 api-key 설정을 보고 자동으로 빈을 만들어 주므로, 여기서는
 * 그 빈을 받아 VectorStore 로 감싸기만 하면 된다.
 */
@Configuration
public class Lab2VectorStoreConfig {

    @Bean
    VectorStore vectorStore(EmbeddingModel embeddingModel) {
        // 인메모리 — 저장된 벡터는 애플리케이션을 재시작하면 비워진다.
        // 재시작해도 남아야 하면 pgvector 등 영속 스토어로 교체한다(9장 확장 과제).
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
