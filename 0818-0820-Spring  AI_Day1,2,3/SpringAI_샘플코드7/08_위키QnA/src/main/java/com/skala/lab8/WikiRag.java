package com.skala.lab8;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

/**
 * 8장 미니 실습 — 우리 팀 위키 Q&A.
 *
 * <p>인제스트를 두 번 실행해 조각 수가 늘어나는지 본다. 늘어난다면 재색인이 빠진 것이다.
 * 답보다 검색 결과를 먼저 확인하는 습관을 여기서 들인다.
 */
@Service
public class WikiRag {

    private static final Logger log = LoggerFactory.getLogger(WikiRag.class);

    private final VectorStore store;
    private final ChatClient chat;

    public WikiRag(VectorStore store, ChatClient.Builder builder) {
        this.store = store;
        this.chat = builder.build();
    }

    /** 인제스트 — 같은 출처를 지우고 다시 넣는다(= 재색인). */
    public int 넣기(String 파일명, String 본문) {
        var doc = new Document(본문, Map.of("source", 파일명, "version", "v1"));
        var 조각 = new TokenTextSplitter().apply(List.of(doc));
        store.delete(new FilterExpressionBuilder().eq("source", 파일명).build());
        store.add(조각);
        log.info("{} → {}조각", 파일명, 조각.size());
        return 조각.size();
    }

    /** 검색만 — 답변을 만들기 전에 이것부터 눈으로 본다. */
    public List<Document> 찾기(String q) {
        List<Document> 근거 = store.similaritySearch(SearchRequest.builder()
                .query(q).topK(3).similarityThreshold(0.5).build());
        근거.forEach(d -> log.info(" 근거 {} ({})", d.getMetadata().get("source"), d.getScore()));
        return 근거;
    }

    public String 묻기(String q) {
        List<Document> 근거 = 찾기(q);
        if (근거.isEmpty()) {
            return "확인되지 않습니다.";                      // 없으면 모델을 안 부른다
        }
        return chat.prompt()
                .system("아래 근거만 사용해 답한다. 없으면 '확인되지 않습니다'.")
                .user("[근거]%n%s%n[질문] %s".formatted(합치기(근거), q))
                .call().content();
    }

    private String 합치기(List<Document> 근거) {
        return 근거.stream()
                .map(d -> "- (%s) %s".formatted(d.getMetadata().get("source"), d.getText()))
                .collect(Collectors.joining("\n"));
    }
}
