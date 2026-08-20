package com.skala.lab9;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 인메모리 벡터 저장소.
 *
 * <p>따로 설치할 것이 없다. 앱을 끄면 넣어 둔 문서도 사라지므로, 다시 띄우면 인제스트부터 한다.
 * 운영에서는 pgvector 같은 실제 저장소로 바꾼다.
 */
@Configuration
public class VectorStoreConfig {

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        return SimpleVectorStore.builder(embeddingModel).build();
    }
}
