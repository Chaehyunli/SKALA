package com.skala.lab0.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.skala.lab0.service.SnackService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 1장 미니 실습 — 요청을 받아 서비스에 넘기기만 한다.
 *
 * <p>키가 없어도 끝까지 돌아간다. AI 없이 계층만 확인하는 실습이다.
 * 확인: {@code curl 'localhost:8080/lab0/snack?mood=피곤'}
 */
@RestController
@RequestMapping("/lab0/snack")
@Tag(name = "1장 실습 · 간식 추천")
public class SnackController {

    private final SnackService service;   // 저장소는 모른다

    public SnackController(SnackService service) {
        this.service = service;
    }

    @GetMapping                            // GET /lab0/snack?mood=피곤
    @Operation(summary = "기분에 맞는 간식 추천", description = "AI 를 쓰지 않는다. 키 없이 동작한다.")
    public SnackResponse pick(@RequestParam String mood) {
        return service.recommend(mood);
    }
}
