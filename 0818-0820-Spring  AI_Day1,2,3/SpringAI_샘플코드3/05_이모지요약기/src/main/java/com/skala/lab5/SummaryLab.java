package com.skala.lab5;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * 5장 미니 실습 — 이모지 한 줄 요약기.
 *
 * <p>v1 과 v2 를 나란히 호출해 프롬프트만 바꾼 차이를 본다.
 * 모델도 온도도 그대로다. 바뀐 것은 지시서뿐이다.
 */
@RestController
@Tag(name = "5장 실습 · 이모지 요약기")
public class SummaryLab {

    private static final Logger log = LoggerFactory.getLogger(SummaryLab.class);

    private final ChatClient chat;

    public SummaryLab(ChatClient.Builder b) {
        this.chat = b.build();
    }

    /** 기준선 — 대충 시킨다. 길이도 모양도 매번 다르다. */
    @GetMapping("/lab5/v1")
    @Operation(summary = "기준선 요약(대충 시킨 프롬프트)")
    public String v1(@RequestParam String text) {
        return chat.prompt().user("이 글 요약해줘: " + text).call().content();
    }

    /** 4요소로 다시 쓴다 — 역할 · 지시 · 예시 · 출력 형식. */
    @GetMapping("/lab5/v2")
    @Operation(summary = "형식을 못 박은 요약", description = "이모지 3개 + 20자 이내로 고정된다.")
    public String v2(@RequestParam String text) {
        return chat.prompt()
                .system("""
                        너는 한 줄 요약가다.
                        출력 형식(반드시 지킨다):  이모지3개 | 20자 이내 요약
                        예) 오늘 배포 실패로 밤샘했다   →  😱🌙💻 | 배포 실패로 밤샘
                        예) 점심에 마라탕 먹고 행복했다 →  🌶️🍜😊 | 마라탕으로 행복
                        """)
                .user(text)
                .options(ChatOptions.builder().temperature(0.0).build())   // 매번 같은 답
                .call().content();
    }

    /**
     * 스트리밍 — 첫 글자까지 몇 ms 걸리는지 재 본다.
     *
     * <p>전체가 3초여도 첫 글자가 0.5초면 사람은 빠르다고 느낀다.
     */
    @GetMapping("/lab5/stream")
    @Operation(summary = "첫 글자까지의 시간 측정")
    public Map<String, Object> stream(@RequestParam String text) {
        long t0 = System.currentTimeMillis();
        AtomicBoolean 처음 = new AtomicBoolean(true);
        StringBuilder 전체 = new StringBuilder();
        long[] 첫글자 = {-1};

        chat.prompt().user(text).stream().content()
                .doOnNext(tok -> {
                    if (처음.getAndSet(false)) {
                        첫글자[0] = System.currentTimeMillis() - t0;
                        log.info("첫 글자까지 {}ms", 첫글자[0]);
                    }
                    전체.append(tok);
                })
                .blockLast();

        return Map.of("첫글자ms", 첫글자[0],
                      "전체ms", System.currentTimeMillis() - t0,
                      "답", 전체.toString());
    }
}
