package com.skala.day2.web;

import com.skala.day2.service.Lab2IngestService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 문서 Q&A(Lab2) 파이프라인의 입구 — 인제스트 API.
 *
 * <p>바디 없이 호출하면 lab2-docs 아래 문서를 전부 (재)인제스트한다.
 * 검색·답변 API(Step2, Step3)를 쓰기 전에 반드시 한 번 호출해야 벡터스토어에 데이터가 생긴다.
 *
 * <p>확인: {@code curl -X POST localhost:8080/lab2/ingest}
 * → {@code [{"source":"return-policy","chunks":N}, ...]}
 */
@RestController
@Tag(name = "Day2 실습 · 문서 Q&A")
public class Lab2IngestController {

    private final Lab2IngestService ingestService;

    public Lab2IngestController(Lab2IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/lab2/ingest")
    public List<IngestResult> ingest() {
        // 요청 파라미터 없음 — lab2-docs 폴더 전체를 대상으로 한다(문서 단위 선택 인제스트는 지금 범위 밖).
        return ingestService.ingestAll();
    }
}
