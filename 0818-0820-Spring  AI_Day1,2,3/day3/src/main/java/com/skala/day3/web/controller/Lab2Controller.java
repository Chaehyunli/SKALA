package com.skala.day3.web.controller;

import com.skala.day3.service.Lab2AskService;
import com.skala.day3.service.Lab2IngestService;
import com.skala.day3.service.Lab2RetrieveService;
import com.skala.day3.web.dto.lab2.Chunk;
import com.skala.day3.web.dto.lab2.request.AskRequest;
import com.skala.day3.web.dto.lab2.response.AnswerDto;
import com.skala.day3.web.dto.lab2.response.IngestResult;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * RAG 파이프라인(Lab2) 전체 — 인제스트 → 검색 → 답변.
 */
@RestController
@Tag(name = "Day2 실습 · 문서 Q&A")
public class Lab2Controller {

    private final Lab2IngestService ingestService;
    private final Lab2RetrieveService retrieveService;
    private final Lab2AskService askService;

    public Lab2Controller(Lab2IngestService ingestService,
                           Lab2RetrieveService retrieveService,
                           Lab2AskService askService) {
        this.ingestService = ingestService;
        this.retrieveService = retrieveService;
        this.askService = askService;
    }

    /**
     * 1단계 — 인제스트 입구.
     *
     * <p>바디 없이 호출하면 lab2-docs 아래 문서를 전부 (재)인제스트한다.
     * 검색·답변 API를 쓰기 전에 반드시 한 번 호출해야 벡터스토어에 데이터가 생긴다.
     *
     * <p>확인: {@code curl -X POST localhost:8080/lab2/ingest}
     * → {@code [{"source":"return-policy","chunks":N}, ...]}
     */
    @PostMapping("/lab2/ingest")
    public List<IngestResult> ingest() {
        // 요청 파라미터 없음 — lab2-docs 폴더 전체를 대상으로 한다(문서 단위 선택 인제스트는 지금 범위 밖).
        return ingestService.ingestAll();
    }

    /**
     * 2단계 — 검색을 먼저 눈으로 본다.
     *
     * <p>답변 API를 만들기 전에 이 API로 먼저 검색 품질을 확인한다.
     * 점수를 감추면 "그럴듯해 보이는데 사실은 근거가 아닌" 답변을 걸러낼 수 없으므로,
     * 응답에는 항상 유사도 점수를 그대로 노출한다.
     *
     * <p>확인: {@code curl 'localhost:8080/lab2/retrieve?q=반품 기한&threshold=0.3'}
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

    /**
     * 3단계 — 근거로 답하기.
     *
     * <p>확인: {@code curl -X POST localhost:8080/lab2/ask -H 'Content-Type: application/json'
     * -d '{"question":"단순 변심 반품은 며칠 이내인가요?","threshold":0.3}'}
     * topK·threshold 는 생략하면 각각 4, 0.5 가 적용된다(AskRequest 기본값).
     */
    @PostMapping("/lab2/ask")
    public AnswerDto ask(@RequestBody AskRequest request) {
        return askService.ask(request.question(), request.topK(), request.threshold());
    }

    /** 청크 본문이 길면 화면에서 확인하기 번거로우니 앞부분만 잘라 미리보기로 보여준다. */
    private String snippet(String text, int maxLen) {
        if (text == null || text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen) + "...";
    }
}
