package com.skala.day2.web.controller;

import com.skala.day2.service.Lab2RetrieveService;
import com.skala.day2.web.dto.Chunk;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RAG 파이프라인의 2단계 — 검색을 먼저 눈으로 본다.
 *
 * <p>답변 API(Step3)를 만들기 전에 이 API로 먼저 검색 품질을 확인한다.
 * 점수를 감추면 "그럴듯해 보이는데 사실은 근거가 아닌" 답변을 걸러낼 수 없으므로,
 * 응답에는 항상 유사도 점수를 그대로 노출한다.
 */
@RestController
@Tag(name = "Day2 실습 · 문서 Q&A")
public class Lab2RetrieveController {

    private final Lab2RetrieveService retrieveService;

    public Lab2RetrieveController(Lab2RetrieveService retrieveService) {
        this.retrieveService = retrieveService;
    }

    /**
     * 확인: {@code curl 'localhost:8080/lab2/retrieve?q=반품 기한&threshold=0.3'}
     *
     * <p>아래 세 질문의 결과를 나란히 놓고 비교해 본다.
     * ① "반품 기한"                 → 기대: return-policy 상위
     * ② "물건 돌려보내려면 며칠 안에?" → 기대: 같은 문서 — 표현이 달라도 찾는가
     * ③ "우주 배송"                 → 기대: 점수가 전부 낮다 → 근거 없음
     */
    @GetMapping("/lab2/retrieve")
    public List<Chunk> retrieve(@RequestParam String q,
                                 @RequestParam(defaultValue = "4") int topK,
                                 @RequestParam(defaultValue = "0.5") double threshold) {
        return retrieveService.retrieve(q, topK, threshold).stream()
                .map(d -> new Chunk(
                        d.getMetadata().get("source").toString(),
                        d.getScore(),                 // 점수를 그대로 노출한다
                        snippet(d.getText(), 120)))
                .toList();
    }

    /** 청크 본문이 길면 화면에서 확인하기 번거로우니 앞부분만 잘라 미리보기로 보여준다. */
    private String snippet(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
