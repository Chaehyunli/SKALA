package com.skala.day2.web;

import com.skala.day2.service.Lab2AskService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * RAG 파이프라인의 3단계 — 근거로 답하기.
 *
 * <p>확인: {@code curl -X POST localhost:8080/lab2/ask -H 'Content-Type: application/json'
 * -d '{"question":"단순 변심 반품은 며칠 이내인가요?"}'}
 */
@RestController
@Tag(name = "Day2 실습 · 문서 Q&A")
public class Lab2AskController {

    private final Lab2AskService askService;

    public Lab2AskController(Lab2AskService askService) {
        this.askService = askService;
    }

    @PostMapping("/lab2/ask")
    public AnswerDto ask(@RequestBody AskRequest request) {
        return askService.ask(request.question());
    }
}
