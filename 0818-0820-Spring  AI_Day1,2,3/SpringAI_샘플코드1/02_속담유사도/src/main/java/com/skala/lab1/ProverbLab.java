package com.skala.lab1;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 2장 미니 실습 — 내 말과 닮은 속담 찾기.
 *
 * <p>임베딩이 "뜻을 숫자로 바꾸는 일" 이라는 걸 눈으로 확인한다.
 * 점수 자체보다 <b>순서</b>를 본다. 값은 모델 버전마다 조금씩 달라진다.
 *
 * <p>확인: {@code curl 'localhost:8080/lab1/proverb?q=조심해서 나쁠 건 없지'}
 */
@RestController
@Tag(name = "2장 실습 · 속담 유사도")
public class ProverbLab {

    private final EmbeddingModel embedding;              // 뜻을 숫자로 바꾸는 도구

    public ProverbLab(EmbeddingModel embedding) {
        this.embedding = embedding;
    }

    static final List<String> 속담 = List.of(
            "티끌 모아 태산", "돌다리도 두들겨 보고 건너라",
            "원숭이도 나무에서 떨어진다", "가는 말이 고와야 오는 말이 곱다");

    @GetMapping("/lab1/proverb")                         // GET ?q=조심해서 나쁠 건 없지
    @Operation(summary = "내 문장과 가장 가까운 속담", description = "임베딩 모델을 호출한다. 키가 필요하다.")
    public Map<String, Double> match(@RequestParam String q) {
        float[] 내문장 = embedding.embed(q);               // 내 문장 → 숫자 배열
        Map<String, Double> 점수 = new LinkedHashMap<>();
        for (String p : 속담) {
            점수.put(p, cosine(내문장, embedding.embed(p))); // 속담과 거리 재기
        }
        return 점수;
    }

    /** 두 화살표가 이루는 각도. 1 에 가까울수록 비슷하다. */
    static double cosine(float[] a, float[] b) {
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
