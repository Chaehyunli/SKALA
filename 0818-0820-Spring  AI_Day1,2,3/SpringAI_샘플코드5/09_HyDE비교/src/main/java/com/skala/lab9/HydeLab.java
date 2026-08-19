package com.skala.lab9;

import java.util.List;
import java.util.Map;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 9장 미니 실습 — 못 찾던 질문 고치기(HyDE).
 *
 * <p>8장 실습의 문서가 이미 인제스트돼 있어야 한다({@code POST /lab8/ingest}).
 * 전후를 같은 화면에서 비교하고, 좋아지지 않으면 되돌린다.
 */
@RestController
@Tag(name = "9장 실습 · HyDE 전후 비교")
public class HydeLab {

    private final VectorStore store;
    private final ChatClient chat;

    public HydeLab(VectorStore store, ChatClient.Builder builder) {
        this.store = store;
        this.chat = builder.build();
    }

    @GetMapping("/lab9/compare")
    @Operation(summary = "그냥 검색 vs HyDE", description = "질문 대신 '가상의 답'으로 검색하면 회수율이 오르는지 본다.")
    public Map<String, Object> compare(@RequestParam String q) {
        List<Document> 그냥 = 검색(q);

        // 가상의 답을 먼저 만들고, 그 문장으로 검색한다(사실 여부는 상관없다)
        String 가상답 = chat.prompt()
                .user("다음 질문에 대한 그럴듯한 답을 2문장으로 써라(사실 여부는 상관없다): " + q)
                .call().content();
        List<Document> 개선 = 검색(가상답);

        return Map.of("질문", q,
                      "가상답", 가상답,
                      "그냥검색", 요약(그냥),
                      "HyDE", 요약(개선));
    }

    private List<Document> 검색(String query) {
        return store.similaritySearch(SearchRequest.builder().query(query).topK(3).build());
    }

    private List<Map<String, Object>> 요약(List<Document> docs) {
        return docs.stream()
                .map(d -> Map.<String, Object>of(
                        "source", String.valueOf(d.getMetadata().get("source")),
                        "score", d.getScore(),
                        "앞부분", d.getText() == null ? ""
                                : d.getText().substring(0, Math.min(60, d.getText().length()))))
                .toList();
    }
}
