package com.skala.lab13;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 13장 미니 실습 — 어디가 느린지 재고, 캐시로 줄이기.
 *
 * <p>같은 질문을 스무 번 던져 구간 시간을 모은다. 대개 8할이 모델이다.
 * 우리 코드가 아니라는 뜻이고, 그래서 답은 캐시다.
 */
@RestController
@Tag(name = "13장 실습 · 병목과 캐시")
public class CostLab {

    private static final Logger log = LoggerFactory.getLogger(CostLab.class);

    private final ChatClient chat;
    private final VectorStore store;
    private final Map<String, String> 캐시 = new ConcurrentHashMap<>();

    public CostLab(ChatClient.Builder builder, VectorStore store) {
        this.chat = builder.build();
        this.store = store;
    }

    @GetMapping("/lab13/ask")
    @Operation(summary = "구간 시간을 재며 답한다", description = "cache=true 로 부르면 두 번째부터 밀리초 단위로 끝난다.")
    public String ask(@RequestParam String q,
                      @RequestParam(defaultValue = "false") boolean cache) {
        if (cache && 캐시.containsKey(q)) {
            log.info("캐시 적중 — 모델을 부르지 않는다");
            return 캐시.get(q);
        }

        long t0 = System.nanoTime();
        var 근거 = store.similaritySearch(SearchRequest.builder().query(q).topK(3).build());
        long 검색 = (System.nanoTime() - t0) / 1_000_000;                 // 검색 구간

        long t1 = System.nanoTime();
        String 답 = chat.prompt().user(프롬프트(q, 근거.size())).call().content();
        long 모델 = (System.nanoTime() - t1) / 1_000_000;                 // 모델 구간

        log.info("검색 {}ms · 모델 {}ms · 합계 {}ms", 검색, 모델, 검색 + 모델);
        if (cache) {
            캐시.put(q, 답);
        }
        return 답;
    }

    private String 프롬프트(String q, int 근거수) {
        return "질문: %s (참고 근거 %d건)".formatted(q, 근거수);
    }
}
