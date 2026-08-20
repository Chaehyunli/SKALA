package com.skala.day3.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * RAG 파이프라인의 2단계 — 검색.
 *
 * <p>검색 API(Lab2Controller)와 답변 API(Lab2AskService)가 같은 검색 로직을 쓴다.
 * 두 곳에서 vectorStore 호출을 각자 만들면 조건이 갈라지기 쉬워서 이 서비스 하나로 모은다.
 */
@Service
public class Lab2RetrieveService {

    private final VectorStore vectorStore;

    public Lab2RetrieveService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String query, int topK, double threshold) {
        return vectorStore.similaritySearch(SearchRequest.builder()
                .query(query).topK(topK)
                .similarityThreshold(threshold)   // 낮은 점수는 근거가 아니다
                .build());
    }
}
