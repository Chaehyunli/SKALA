package com.skala.lab9;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 8장 미니 실습 — 인제스트 · 검색 · 답변을 각각 눈으로 확인한다.
 *
 * <p>문서는 {@code src/main/resources/lab8-docs/} 에 있다.
 */
@RestController
@Tag(name = "8장 실습 · 위키 Q&A")
public class WikiRagController {

    private final WikiRag rag;

    public WikiRagController(WikiRag rag) {
        this.rag = rag;
    }

    /** 두 번 실행해 조각 수를 비교한다 — 같아야 정상(재색인이 걸려 있다). */
    @PostMapping("/lab8/ingest")
    @Operation(summary = "lab8-docs 전체 인제스트", description = "두 번 실행해도 조각 수가 늘지 않아야 한다.")
    public List<Map<String, Object>> ingest() throws IOException {
        Resource[] files = new PathMatchingResourcePatternResolver()
                .getResources("classpath:lab8-docs/*.md");
        List<Map<String, Object>> result = new ArrayList<>();
        for (Resource f : files) {
            String name = f.getFilename() == null ? "unknown.md" : f.getFilename();
            String body = new String(f.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            result.add(Map.of("source", name, "chunks", rag.넣기(name, body)));
        }
        return result;
    }

    /** 답변보다 먼저 만든다 — 검색이 보이지 않으면 어디를 고칠지 알 수 없다. */
    @GetMapping("/lab8/retrieve")
    @Operation(summary = "검색 결과만 본다", description = "출처와 점수를 그대로 노출한다.")
    public List<Map<String, Object>> retrieve(@RequestParam String q) {
        return rag.찾기(q).stream()
                .map(d -> Map.<String, Object>of(
                        "source", String.valueOf(d.getMetadata().get("source")),
                        "score", d.getScore(),
                        "snippet", d.getText() == null ? ""
                                : d.getText().substring(0, Math.min(120, d.getText().length()))))
                .toList();
    }

    @GetMapping("/lab8/ask")
    @Operation(summary = "근거를 붙여 답한다", description = "근거가 없으면 모델을 부르지 않는다.")
    public Map<String, String> ask(@RequestParam String q) {
        return Map.of("answer", rag.묻기(q));
    }
}
